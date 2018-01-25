/**
 * 显示视频的样例代码
 */

#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>

#include <signal.h>
#include <termios.h>
#include <stdio.h>
#include "boost/thread/mutex.hpp"
#include "boost/thread/thread.hpp"



#include <stdlib.h>
#include <unistd.h>




#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/select.h>
#include <termios.h>


#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <sys/time.h>
#include <stdexcept>
#include <std_srvs/Empty.h>


#include <iostream>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <unistd.h>
#include <string.h>
#include <thread>
#include <iostream>
#include <cstdio>
#include <thread>

#include <stdexcept>
#include <stdio.h>
#include <string>
#include <fstream>
#include <sstream>
#include <unistd.h>

using namespace std;

char*       serverIP; //机器人IP地址
int         serverPort=4108;  // 该端口用来遥控机器人


#define KEYCODE_R 0x43
#define KEYCODE_L 0x44
#define KEYCODE_U 0x41
#define KEYCODE_D 0x42
#define KEYCODE_Q 0x71


int x = 0 , z = 0 ; 

int ispeed =1 ; //轮子转速等级 1,2,3,4,5 慢到快

/*全局速度控制信息  

x=+1 向前走 
x=0  线速度为零
x=-1 向后走

z=+1 左传
z=0  角速度为零
z=-1 右转

*/


 




void sendVelocity ( int sokt ) {

    int bytes;


     while ( true) { 

         

        usleep( 100  * 1000); // sleep 0.1s 


       	int scaled_x = x*ispeed;

	int scaled_z = z*ispeed;
	

        if ((bytes = send(sokt, &scaled_x, sizeof(scaled_x) , 0 )) < 0 ) {
            std::cerr << "send failed, send bytes = " << bytes << std::endl;
		 continue;
        }else {


	

	}

        if ((bytes = send(sokt, &scaled_z, sizeof(scaled_z) , 0 )) < 0 ) {
            std::cerr << "send failed, send bytes = " << bytes << std::endl;
		continue;
        }


    }


}





struct termios orig_termios;

void reset_terminal_mode()
{
    tcsetattr(0, TCSANOW, &orig_termios);
}

void set_conio_terminal_mode()
{
    struct termios new_termios;

    /* take two copies - one for now, one for later */
    tcgetattr(0, &orig_termios);
    memcpy(&new_termios, &orig_termios, sizeof(new_termios));

    /* register cleanup handler, and set the new terminal mode */
    atexit(reset_terminal_mode);
    cfmakeraw(&new_termios);
    tcsetattr(0, TCSANOW, &new_termios);
}

int kbhit()
{
    struct timeval tv = { 0L, 0L };
    fd_set fds;
    FD_ZERO(&fds);
    FD_SET(0, &fds);
    return select(1, &fds, NULL, NULL, &tv);
}

int getch()
{
    int r;
    unsigned char c;
    if ((r = read(0, &c, sizeof(c))) < 0) {
        return r;
    } else {
        return c;
    }
}










int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------

    string str = argv [1] ;  // 机器人IP以参数方式传入



    string speed =  argv[2] ; //轮子转速等级 1,2,3,4,5 慢到快



    try {    

	ispeed=std::stoi( speed );

    }catch ( const std::exception &e) {

	cout << "wrong argment , need integer" << endl; 

    }

    cout << "ispeed" << ispeed << endl;

    serverIP = new char [ str.size() + 1  ] ;

    serverIP[str.size()]= '\0' ;

    std::copy(str.begin(), str.end(), serverIP) ;


    cout << "serverIP " << serverIP << endl;

    int         sokt;

    struct  sockaddr_in serverAddr;
    socklen_t           addrLen = sizeof(struct sockaddr_in);

    if ((sokt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
        std::cerr << "socket() failed" << std::endl;
    }

    serverAddr.sin_family = PF_INET;
    serverAddr.sin_addr.s_addr = inet_addr(serverIP);
    serverAddr.sin_port = htons(serverPort);

    if (connect(sokt, (sockaddr*)&serverAddr, addrLen) < 0) {
        std::cerr << "connect() failed!" << std::endl;
    }




    int bytes = 0;
    int key=-1;




    set_conio_terminal_mode();

    
    std::thread t (sendVelocity , std::ref(sokt)) ; 
    t.detach() ; 


    


    while ( true ) {

    char c = getch(); /*获取键盘输入，更改速度 */

 

    switch(c)
	    {
	      case 'd':

		x=0;z = -1;
		break;



	      case 'a':

		x=0;z = 1;
		break;

	      case 'q':

		x = 1 ; z = 1;
		break;



	      case 'w':

		x = 1;z=0;
		break;
	      case 'x':

		x = -1;z=0;
		break;



	      case 'e':

		x = 1 ; z= -1;
		break;

	      case 'z':

		x = -1; z= -1 ;
		break;

	      case 'c':

		x = -1; z=1 ;
		break;


	      case 's':

		x = 0; z=0 ;
		break;


	     case  'r' : return 0 ; // 退出程序

		default :  break;

	    }




    }

    usleep ( 100000) ;

    close(sokt);
    return 0;
}


