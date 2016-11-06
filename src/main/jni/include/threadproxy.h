#ifndef __THREAD_PROXY_H__
#define __THREAD_PROXY_H__
#include <pthread.h>
/**
 * 封装线程
 */
template<typename T>
class ThreadProxy{
public:
    struct Runnable{

        Runnable( T* h, void*(T::*r)(void*), void* a ) : host(h), rtn(r), args(a) {
        }

        void *operator()(){
            return (host->*rtn)(args);
        }

        ~Runnable(){}
    private:
        T *host;
        void* (T::*rtn)(void*);
        void* args;
    };

    /**
     * 创建线程
     */
    static int create( pthread_t *, pthread_attr_t *, const Runnable *  );

private:
    static void *threadRtn( void* );
};

template<typename T>
int ThreadProxy<T>::create( pthread_t *thread, pthread_attr_t *attr, const Runnable* param ){
    return pthread_create( thread, attr, threadRtn, (void*)param);
}

template<typename T>
void* ThreadProxy<T>::threadRtn( void* args ){
    Runnable *r = (Runnable*)args;
    void* ret = NULL;
    if( NULL != args ){
        ret = (*r)();
    }
    return ret;
}
#endif
