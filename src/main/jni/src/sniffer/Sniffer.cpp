
#include "../../include/log.h"
#include "../../include/utils.h"
#include "../../include/libnids/scan.h"
#include "../../include/event.h"
#include "../../include/sniffer/sniffer.h"

static struct nids_chksum_ctl g_checksum_ctrl;

extern char SEMA_PUT_PATH[256]; // "/data/data/jupiter.sniffer/put"
extern char SEMA_GET_PATH[256]; // "/data/data/jupiter.sniffer/get"
extern char SEMA_MUTEX_PATH[256]; // "/data/data/jupiter.sniffer/mutex"
extern char SHM_PATH[256]; // "/data/data/jupiter.sniffer/shm"

static Sniffer &g_sniffer = Sniffer::get_instance();

/**
 * nids检测到的告警
 */
static void my_nids_syslog(int type, int errnum, struct ip *iph, void *data) {
	char saddr[20], daddr[20];
	char buf[1024];
	struct host *this_host;
	unsigned char flagsand = 255, flagsor = 0;
	int i;
	switch (type) {
		case NIDS_WARN_IP: {
			LOGE("NIDS WARN:IP");
			Packet pkt;
			pkt.m_type = 0;
			pkt.m_event = ENV_WARN_IP;
			pkt.m_length = 0;
			pkt.m_extra = errnum;
			char *src = int_ntoa(iph->ip_src.s_addr);
			char *dst = int_ntoa(iph->ip_dst.s_addr);
			if (NULL != src ) {
				strcpy(pkt.m_src_ip,  src);
			}
			if (NULL != dst) {
				strcpy(pkt.m_dest_ip, dst);
			}
			g_sniffer.notify_packet(pkt);
			break;
		}
		case NIDS_WARN_TCP: {
			LOGE("NIDS WARN:TCP");
			Packet pkt;
			pkt.m_type = 0;
			pkt.m_event = ENV_WARN_TCP;
			pkt.m_length = 0;
			pkt.m_extra = errnum;
			pkt.m_src_port  = ntohs(((struct tcphdr *) data)->source);
			pkt.m_dest_port = ntohs(((struct tcphdr *) data)->dest);
			const char *src = int_ntoa(iph->ip_src.s_addr);
			const char *dst = int_ntoa(iph->ip_dst.s_addr);
			if (NULL != src && NULL != dst) {
				strcpy(pkt.m_src_ip,  src);
				strcpy(pkt.m_dest_ip, dst);
			}
			g_sniffer.notify_packet(pkt);
			break;
		}
		case NIDS_WARN_SCAN: {
			LOGE("NIDS WARN:SCAN");
			this_host = (struct host *) data;
			sprintf(buf, "Scan from %s.",
					int_ntoa(this_host->addr));
			for (i = 0; i < this_host->n_packets; i++) {
				//strcat(buf, int_ntoa(this_host->packets[i].addr));
				//sprintf(buf + strlen(buf), ":%hu,",
				//		this_host->packets[i].port);
				flagsand &= this_host->packets[i].flags;
				flagsor |= this_host->packets[i].flags;
			}
			if (flagsand == flagsor) {
				i = flagsand;
				switch (flagsand) {
					case 2:
						strcat(buf, "scan type: SYN");
						break;
					case 0:
						strcat(buf, "scan type: NULL");
						break;
					case 1:
						strcat(buf, "scan type: FIN");
						break;
					default:
						sprintf(buf + strlen(buf), "flags=0x%x", i);
				}
			} else {
				strcat(buf, "various flags");
			}
			Packet pkt;
			pkt.m_type = 0;
			pkt.m_event = ENV_WARN_SCAN;
			pkt.m_length = strlen(buf);
			strcpy(pkt.m_data, buf);
			g_sniffer.notify_packet(pkt);
			break;
		}
		default:
			break;
	}
}

/**
 * nids抓到tcp包的回调函数,这里通过共享内存将抓到的数据传递给android
 */
static void tcp_callback(struct tcp_stream *a_tcp, void **data) {
	char buf[1024];
	strcpy (buf, adres (a_tcp->addr)); // we put conn params into buf
	if (a_tcp->nids_state == NIDS_JUST_EST) {
	    // connection described by a_tcp is established
	    // here we decide, if we wish to follow this stream
	    // sample condition: if (a_tcp->addr.dest!=23) return;
	    // in this simple app we follow each stream, so..
	    a_tcp->client.collect++; // we want data received by a client
	    a_tcp->server.collect++; // and by a server, too
	    a_tcp->server.collect_urg++; // we want urgent data received by a server
	#ifdef WE_WANT_URGENT_DATA_RECEIVED_BY_A_CLIENT
	      a_tcp->client.collect_urg++; // if we don't increase this value,
	                                   // we won't be notified of urgent data
	                                   // arrival
	#endif
	      LOGE("%s established\n", buf);

          Packet pkt;
		  pkt.m_type = 0;
          pkt.m_event = ENV_NEW_TCP_CONNECTION;
          pkt.m_length = 0;
          pkt.m_src_port = a_tcp->addr.source;
          pkt.m_dest_port = a_tcp->addr.dest;
          strcpy(pkt.m_src_ip, int_ntoa(a_tcp->addr.saddr));
          strcpy(pkt.m_dest_ip, int_ntoa(a_tcp->addr.daddr));
		  g_sniffer.notify_packet(pkt);
	      return;
	}

	if (a_tcp->nids_state == NIDS_CLOSE || a_tcp->nids_state == NIDS_RESET) {
		// connection has been closed normally
		LOGE("%s closing\n", buf);
        int event = (a_tcp->nids_state == NIDS_CLOSE ? ENV_TCP_CONNECTION_CLOSE : ENV_TCP_CONNECTION_CLOSED_BY_RST);
        Packet pkt;
		pkt.m_type = 0;
        pkt.m_event = event;
        pkt.m_length = 0;
        pkt.m_src_port = a_tcp->addr.source;
        pkt.m_dest_port = a_tcp->addr.dest;
        strcpy(pkt.m_src_ip, int_ntoa(a_tcp->addr.saddr));
        strcpy(pkt.m_dest_ip, int_ntoa(a_tcp->addr.daddr));
		g_sniffer.notify_packet(pkt);
		return;
	}

	bool toServer = false;
	if (a_tcp->nids_state == NIDS_DATA) {
		// new data has arrived; gotta determine in what direction
		// and if it's urgent or not
		struct half_stream *hlf;
		if (a_tcp->server.count_new_urg) {
	        // new byte of urgent data has arrived
	        strcat(buf,"(urgent->)");
	        buf[strlen(buf)+1]=0;
	        buf[strlen(buf)]=a_tcp->server.urgdata;
	        LOGE("%s", buf);
	        return;
		}
		// We don't have to check if urgent data to client has arrived,
		// because we haven't increased a_tcp->client.collect_urg variable.
		// So, we have some normal data to take care of.
		if (a_tcp->client.count_new) {
			// new data for client
			hlf = &a_tcp->client; // from now on, we will deal with hlf var,which will point to client side of con
		} else {
		  hlf = &a_tcp->server; // analogical
		  toServer = true;
		}

	    LOGE("data on conn:%s",buf); // we print the connection parameters (saddr, daddr, sport, dport) accompanied
	                    // by data flow direction (-> or <-)
	    //1.copy address tuple and data to buffer
        Packet pkt;
		pkt.m_type = 0;
        pkt.m_event = (toServer ? ENV_NEW_DATA_TO_SERVER : ENV_NEW_DATA_FROM_SERVER);
        pkt.m_src_port = a_tcp->addr.source;
        pkt.m_dest_port = a_tcp->addr.dest;
        pkt.m_length = hlf->count_new;
        pkt.m_ack_seq = hlf->ack_seq;
        pkt.m_acked = hlf->acked;
        pkt.m_seq = hlf->seq;
        pkt.m_window = hlf->window;
        memcpy(pkt.m_data, hlf->data, hlf->count_new);
        strcpy(pkt.m_src_ip, int_ntoa(a_tcp->addr.saddr));
        strcpy(pkt.m_dest_ip, int_ntoa(a_tcp->addr.daddr));
		g_sniffer.notify_packet(pkt);
	}
	return ;
}

/**
 * nids抓到udp包以后的回调,这里通过共享内存将抓到的数据传递给android
 */
static void udp_callback(struct tuple4 *addr,char *buf,int len,struct ip * iph) {
	if (addr == NULL || len <= 0 || buf == NULL) return;
	Packet pkt;
	pkt.m_type = 1;
	pkt.m_event = ENV_NEW_UDP_DATA;
	strcpy(pkt.m_src_ip, int_ntoa(addr->saddr));
	strcpy(pkt.m_dest_ip, int_ntoa(addr->daddr));
	pkt.m_src_port = addr->source;
	pkt.m_dest_port = addr->dest;
	pkt.m_length = len;
	memcpy(pkt.m_data, buf, len);
	g_sniffer.notify_packet(pkt);
}

/**
 * 启动sniffer
 * 1.准备IPC通道(共享内存的映射)
 * 2.启动nids
 */
int Sniffer::start_sniff( int argc, char **argv, bool async ) {
	parse_param(argc, argv);
	prepare_sniffer();
    if (async) {
        m_runnable = new ThreadProxy<Sniffer>::Runnable(this, &Sniffer::pcap_thread, NULL);
        ThreadProxy<Sniffer>::create(&m_tid, NULL, m_runnable);
    } else {
        nids_run();
    }
    return 0;
}

void Sniffer::parse_param(int argc, char **argv) {
	if (argc <= 0) {
		LOGE("No params specified");
		return;
	}

	for (int i = 0; i < argc; i++) {
		char *opt = argv[i];
		//过滤表达式
		if (opt != NULL && strcmp(opt, "-p") == 0) {
			char *pcap_filter = new char[256];
			int j = i + 1;
			int len = 0;
			while ( j < argc && strcmp( argv[j], "pend") != 0) {
				strcat(pcap_filter + len, argv[j]);
				len += strlen(argv[j]);
				strcat(pcap_filter + len, " ");
				len++;
				j++;
			}
			pcap_filter[len] = 0;
			LOGE("pcap filter = %s", pcap_filter);
			nids_params.pcap_filter = pcap_filter;
		}
		//校验和选项
		if (opt != NULL && strcmp(opt, "-checksum") == 0) {
			if (i + 1 < argc) {
				if (strcmp(argv[i + 1], "1") == 0) {
					nids_params.checksum = 1;
				} else {
					nids_params.checksum = 0;
				}
			} else {
				nids_params.checksum = 1;
			}
		}
		//TODO Other params if need
	}
}

/**
 * 设置回调
 */
void Sniffer::set_interface( const SnifferInterface &snifferInterface) {
    m_interface = snifferInterface;
}

/**
 * 关闭校验和检查,某些机型上开启校验和将导致无法抓到包
 */
void Sniffer::close_checksum() {
	g_checksum_ctrl.netaddr = 0;
	g_checksum_ctrl.mask = 0;
	g_checksum_ctrl.action = 1;
	nids_register_chksum_ctl(&g_checksum_ctrl, 1);
}

int Sniffer::prepare_sniffer() {
	//1.close checksum if needed
	if (!nids_params.checksum) {
		close_checksum();
	}

	//2.reset nids_syslog callback
	nids_params.syslog = my_nids_syslog;
	nids_params.promisc = 1;

	//3.初始化nids
	int ret = nids_init();
	if ( ret < 0 ) {
		LOGE("-->Init nids error.");
		exit(1);
	} else if (ret == NIDS_ERROR_INVALID_FILTER) {
		//返回值为2表明用户传入的过滤表达式有错误,通知用户并用默认得过滤表达式启动nids库
		Packet pkt;
		pkt.m_type = 1;
		pkt.m_event = ENV_ERROR_FILTER_EXPR;
		pkt.m_length = 0;
		notify_packet(pkt);
	}

	//4.callback
	nids_register_tcp(tcp_callback);
	nids_register_udp(udp_callback);

	return 0;
}

Sniffer::Sniffer() {

}

void *Sniffer::pcap_thread(void *args) {
    nids_run();
    return 0;
}

void Sniffer::notify_packet(const Packet &pkt) {
	if (m_interface.on_new_packet != NULL) {
		m_interface.on_new_packet(pkt);
	}
}

void Sniffer::stop_sniff() {
}
