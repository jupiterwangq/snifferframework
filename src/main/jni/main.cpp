#include <log.h>
#include <Packet.h>
#include <event.h>
#include <ipc/connection.h>
#include <path.h>
#include "include/utils.h"
#include "include/sniffer/sniffer.h"
#include "include/log.h"

//抓包守护进程的入口，需要以root用户来运行此程序，否则没有权限

extern nids_prm nids_params;

/** IPC通道 */
static IPCConnection<ShmProducer, Packet, Path> *g_conn = NULL;

/**
 * 建立IPC通道
 */
static bool prepare_ipc() {
    if( g_conn != NULL ){
        LOGE("-->ipc channel already exist!");
        return true;
    }

    g_conn = new IPCConnection<ShmProducer, Packet, Path>();

    if( !g_conn->create()){
        LOGE("-->could not create ipc connection.");
        delete g_conn;
        return false;
    }

    return true;
}

static void on_new_pkt(const Packet& pkt) {
    g_conn->write(const_cast<Packet*>(&pkt));
}

static void notify_pid() {
    Packet pkt;
    pkt.m_type = 1;
    pkt.m_event = ENV_PID;
    pkt.m_extra = getpid();
    pkt.m_length = 0;
    LOGE("notify pid:%d", pkt.m_extra);
    on_new_pkt(pkt);
}

static SnifferInterface g_interface = {
       on_new_pkt
};

void generate_path(int argc, char**argv) {
    for (int i = 0; i < argc; i++) {
        char *opt = argv[i];
        if (opt != NULL && strcmp(opt, "-path")) {
            if (i + 1 < argc) {
                argv++;
                const char *path = *argv;
                int len = 0;
                int path_len = strlen(path);
                strncpy(Path::sema_put_file_path,   path, path_len);
                strncpy(Path::sema_get_file_path,   path, path_len);
                strncpy(Path::sema_mutex_file_path, path, path_len);
                strncpy(Path::shm_file_path,        path, path_len);
                len += strlen(path);

                strncat(Path::sema_put_file_path + len,   Path::PUT,   strlen(Path::PUT));
                strncat(Path::sema_get_file_path + len,   Path::GET,   strlen(Path::GET));
                strncat(Path::sema_mutex_file_path + len, Path::MUTEX, strlen(Path::MUTEX));
                strncat(Path::shm_file_path + len,        Path::SHM,   strlen(Path::SHM));
                LOGE("%s,%s,%s,%s", Path::sema_put_file_path, Path::sema_get_file_path, Path::sema_mutex_file_path, Path::shm_file_path);
            }
        }
    }
}

int main( int argc, char **argv){

	//让抓包进程成为守护进程
	set_daemon();

    //生成IPC需要的各种文件
    generate_path(argc, argv);

    //建立和app间的ipc通道
    if (!prepare_ipc()) {
        return -1;
    }

    notify_pid();

    //启动sniffer
    Sniffer &sniffer = Sniffer::get_instance();
    sniffer.set_interface(g_interface);
    sniffer.start_sniff(argc - 1, ++argv);  //进程名会占据一个参数

    //结束时释放一下
    if (g_conn != NULL) {
        delete g_conn;
    }
    return 0;
}
