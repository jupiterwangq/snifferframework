#include "../include/utils.h"
#include "../include/libnids/nids.h"

extern struct nids_prm nids_params;

/**
 * 将进程设置为守护进程(并不严格的实现)
 */
void set_daemon() {
	int pid=fork();
	if(pid < 0) {
		exit(1);
	}else if(pid > 0) {
		exit(0);
	}

	setsid();

	pid=fork();
	if(pid>0){

		exit(0);
	}else if(pid < 0){
		exit(1);
	}

	close_all_fd();

	chdir("/");

	umask(0);

	return;
}

void close_all_fd() {
	struct rlimit lim;
	unsigned int i;

	if (getrlimit(RLIMIT_NOFILE, &lim) < 0) return;
	if (lim.rlim_cur == RLIM_INFINITY){
		lim.rlim_cur = 1024;
	}

	for (i = 0; i < lim.rlim_cur; i ++) {
		//if (i == 1) continue;
		if (close(i) < 0 && errno != EBADF) return;
	}
}

char *test_malloc(int x) {
  char *ret = (char*)malloc(x);

  if (!ret)
    nids_params.no_mem("test_malloc");

  return ret;
}

char *adres (struct tuple4 addr) {
  static char buf[256];
  strcpy (buf, int_ntoa (addr.saddr));
  sprintf (buf + strlen (buf), ",%i,", addr.source);
  strcat (buf, int_ntoa (addr.daddr));
  sprintf (buf + strlen (buf), ",%i", addr.dest);
  return buf;
}

void register_callback(struct proc_node **procs, void (*x)) {
  struct proc_node *ipp;

  for (ipp = *procs; ipp; ipp = ipp->next)
    if (x == ipp->item)
      return;
  ipp = mknew(struct proc_node);
  ipp->item = x;
  ipp->next = *procs;
  *procs = ipp;
}

void unregister_callback(struct proc_node **procs, void (*x)) {
  struct proc_node *ipp;
  struct proc_node *ipp_prev = 0;

  for (ipp = *procs; ipp; ipp = ipp->next) {
    if (x == ipp->item) {
      if (ipp_prev)
	ipp_prev->next = ipp->next;
      else
	*procs = ipp->next;
      free(ipp);
      return;
    }
    ipp_prev = ipp;
  }
}

