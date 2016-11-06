#include <stdio.h>
#include "../../include/ipc/sema.h"
#include "../../include/log.h"

#define MAX_TRIES 10

sem_t *sem_open(const char *pathname, int oflag, mode_t mode, unsigned int value){
	int fd, i, created, save_errno;
	sem_t *sem, seminit;
	struct stat statbuff;
	pthread_mutexattr_t mattr;
	pthread_condattr_t  cattr;

	created = 0;
	sem = SEM_FAILED;
again:
	if( oflag & O_CREAT ){

		fd = open(pathname, oflag | O_EXCL | O_RDWR, mode | S_IXUSR);
		if( fd < 0){
			if( errno == EEXIST && (oflag & O_EXCL) == 0 ){
				LOGE("-->open err:go to exists");
				goto exists;
			}else{
				return SEM_FAILED;
			}
		}

		created = 1;

		memset(&seminit, 0, sizeof(seminit));
		if( write(fd, &seminit, sizeof(seminit)) != sizeof(seminit)){
			LOGE("-->write err:go to err");
			goto err;
		}

		sem = (sem_t*)mmap(NULL, sizeof(sem_t), PROT_READ | PROT_WRITE,
				MAP_SHARED, fd, 0);
		if(sem == MAP_FAILED){
			LOGE("-->mmap failed:go to err.");
			goto err;
		}

		if( (i = pthread_mutexattr_init(&mattr)) != 0 ){
			LOGE("-->mutex attr init err:go to pthread err.");
			goto pthreaderr;
		}
		pthread_mutexattr_setpshared(&mattr, PTHREAD_PROCESS_SHARED);
		i = pthread_mutex_init(&sem->sem_mutex, &mattr);
		pthread_mutexattr_destroy(&mattr);
		if( i != 0){
			LOGE("-->mutex init err: go to pthread err");
			goto pthreaderr;
		}

		if( (i = pthread_condattr_init(&cattr)) != 0){
			LOGE("-->cond attr init err: go to pthread err");
			goto pthreaderr;
		}
		pthread_condattr_setpshared(&cattr, PTHREAD_PROCESS_SHARED);
		i = pthread_cond_init(&sem->sem_cond, &cattr);
		pthread_condattr_destroy(&cattr);
		if( i != 0){
			LOGE("-->cond init err: go to pthread err");
			goto pthreaderr;
		}

		if( (sem->sem_count = value) > sysconf(_SC_SEM_VALUE_MAX)){
			errno = EINVAL;
			goto err;
		}

		if( fchmod(fd, mode) == -1){
			LOGE("-->fchmod err, go to err");
			goto err;
		}

		close(fd);
		sem->sem_magic = SEM_MAGIC;
		return sem;
	}

exists:
	LOGE("-->enter exists");
	if( (fd = open(pathname, O_RDWR)) < 0){
		if( errno == ENOENT && (oflag & O_CREAT)){
			goto again;
		}
		LOGE("-->exists:goto err 1");
		goto err;
	}
	sem = (sem_t*)mmap(NULL, sizeof(sem_t), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
	if( sem == MAP_FAILED){
		LOGE("-->mmap err:go to err");
		goto err;
	}
	for( int i = 0; i < MAX_TRIES; i++){
		if(stat(pathname, &statbuff) == -1){
			if(errno == ENOENT && (oflag & O_CREAT)){
				close(fd);
				LOGE("-->go to again");
				goto again;
			}
			LOGE("-->exists:goto err 2");
			goto err;
		}
		if((statbuff.st_mode & S_IXUSR) == 0){
			close(fd);
			sem->sem_magic = SEM_MAGIC;
			return sem;
		}
		sleep(1);
	}
	errno = ETIMEDOUT;
	LOGE("-->exists:goto err 3");
	goto err;

pthreaderr:
	LOGE("-->Enter pthreaderr");
	errno = i;

err:
	LOGE("-->Enter err.");
	save_errno = errno;
	if( created ){
		unlink(pathname);
	}
	if( sem != MAP_FAILED){
		munmap(sem, sizeof(sem_t));
	}
	close(fd);
	errno = save_errno;
	return SEM_FAILED;
}

int sem_close( sem_t *sem){
	if( sem->sem_magic != SEM_MAGIC){
		errno = EINVAL;
		return -1;
	}
	if(munmap(sem, sizeof(sem_t)) == -1){
		return -1;
	}
	return 0;
}

int sem_unlink(const char *pathname){
	if( unlink(pathname) == -1){
		return -1;
	}
	return 0;
}

int sem_post(sem_t *sem){
	int n;
	if( sem->sem_magic != SEM_MAGIC){
		errno = EINVAL;
		return -1;
	}
	if( (n = pthread_mutex_lock(&sem->sem_mutex)) != 0){
		errno = n;
		return -1;
	}

	if( sem->sem_count == 0){
		pthread_cond_signal(&sem->sem_cond);
	}
	sem->sem_count++;
	pthread_mutex_unlock(&sem->sem_mutex);
	return 0;
}

int sem_wait(sem_t *sem){
	int n;
	if( sem->sem_magic != SEM_MAGIC){
		errno = EINVAL;
		return -1;
	}
	if( (n = pthread_mutex_lock(&sem->sem_mutex)) != 0){
		errno = n;
		return -1;
	}
	while( sem->sem_count == 0){
		pthread_cond_wait(&sem->sem_cond, &sem->sem_mutex);
	}
	sem->sem_count--;
	pthread_mutex_unlock(&sem->sem_mutex);
	return 0;
}

int sem_getvalue(sem_t *sem, int *pvalue){
	int n;
	if( sem->sem_magic != SEM_MAGIC){
		errno = EINVAL;
		return -1;
	}
	if( (n = pthread_mutex_lock(&sem->sem_mutex)) != 0){
		errno = n;
		return -1;
	}
	*pvalue = sem->sem_count;
	pthread_mutex_unlock(&sem->sem_mutex);
	return 0;
}
