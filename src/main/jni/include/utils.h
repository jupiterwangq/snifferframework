#ifndef __UTIL_H__
#define __UTIL_H__

#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/resource.h>
#include <arpa/inet.h>
#include "../include/libnids/nids.h"

# ifdef __cplusplus
extern "C" {
# endif

#define mknew(x)	(x *)test_malloc(sizeof(x))
#define b_comp(x,y)	(!memcmp(&(x), &(y), sizeof(x)))
#define int_ntoa(x)	inet_ntoa(*((struct in_addr *)&x))

/**
 * libnids data struct
 */
struct proc_node {
  void (*item)();
  struct proc_node *next;
};

/**
 * libnids data struct
 */
struct lurker_node {
  void (*item)();
  void *data;
  char whatto;
  struct lurker_node *next;
};

char *test_malloc(int);

char *adres (struct tuple4 addr);

void register_callback(struct proc_node **procs, void (*x));

void unregister_callback(struct proc_node **procs, void (*x));

void close_all_fd();

/**
 * Make dameon process
 */
void set_daemon();

static inline int
before(u_int seq1, u_int seq2)
{
  return ((int)(seq1 - seq2) < 0);
}

static inline int
after(u_int seq1, u_int seq2)
{
  return ((int)(seq2 - seq1) < 0);
}

# ifdef __cplusplus
}
# endif

#endif
