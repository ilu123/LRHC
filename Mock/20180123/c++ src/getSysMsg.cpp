/**
 * 获取系统信息
 */


#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include "utilities.h"
#include <time.h> 
using namespace std;
using namespace cv;


char*       serverIP;
int         serverPort=4200;  // 该端口用来传输系统信息

int main(int argc, char** argv)
{

    string str = slamBoardIP(); //获取ip

    cout << " server IP:  " << str << endl;      

    serverIP = new char [ str.size() + 1  ] ; 

    serverIP[str.size()]= '\0' ; 

    std::copy(str.begin(), str.end(), serverIP) ;



    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------
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

 



	time_t nowtime;  
	
 

	int reconnect = 0 ;
  

 	while (true) {




		if ( 	    reconnect  ){  // 断开后重连操作
	
	 	    close(sokt);

		 
		    cout << "reconnect" << endl;

		    if ((sokt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
			std::cerr << "socket() failed" << std::endl;

			
		    }

		    serverAddr.sin_family = PF_INET;
		    serverAddr.sin_addr.s_addr = inet_addr(serverIP);
		    serverAddr.sin_port = htons(serverPort);


		    if (connect(sokt, (sockaddr*)&serverAddr, addrLen) < 0) {
			std::cerr << "connect() failed!" << std::endl;
		    }


		}






 
		char msg [1024] ;  // 系统信息

		int bytes = 0 ;


		
		//获取Msg
		if ((bytes = recv(sokt, msg, 1024, 0)) <= 0){
		     std::cerr << "bytes = " << bytes << std::endl;
		     reconnect = 1 ;
		     continue;
		     
		}


		reconnect = 0 ;

		nowtime = time(NULL);
		string msgStr = string(msg).substr(0 , bytes) ; 

		cout << " sysMsg : " + msgStr + " " ;

		cout << nowtime << endl;


	}



    close(sokt);

    delete [] serverIP ; 
    return 0;
}	
