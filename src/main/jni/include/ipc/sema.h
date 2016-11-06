#ifndef __SEMA_H__
#define __SEMA_H__

#include <pthread.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/mman.h>

/**
 * 用于进程同步的信号量,见Unix网络编程卷2
 */
typedef struct {
	pthread_mutex_t sem_mutex;
	pthread_cond_t  sem_cond;
	unsigned int    sem_count;
	int sem_magic;
}sem_t;

#define SEM_MAGIC 0x67458923

#ifdef SEM_FAILED
#undef SEM_FAILED
#define SEM_FAILED ((sem_t*)-1)
#else
#define SEM_FAILED ((sem_t*)-1)
#endif

extern sem_t *sem_open(const char *pathname, int oflag, mode_t mode, unsigned int val);

extern int sem_close(sem_t *sem);

extern int sem_unlink(const char *pathname);

extern int sem_post(sem_t *sem);

extern int sem_wait(sem_t *sem);

extern int sem_getvalue(sem_t *sem, int *pvalue);

#endif
