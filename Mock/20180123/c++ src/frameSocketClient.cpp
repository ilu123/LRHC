/**
 * 显示视频的样例代码
 */

#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>

using namespace cv;
using namespace std;

char*       serverIP; //机器人IP地址
int         serverPort=4096;  // 该端口用来读取视频数据

int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------

    string str = argv [1] ;  // 机器人IP以参数方式传入


    string chmod = argv [2] ; 

   

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



	 int sendMsg =-1 ;


	try {    

		sendMsg=std::stoi( chmod );

	}catch ( const std::exception &e) {

		cout << "wrong argment , need integer" << endl; 

	}

	   int bytes = 0;


	if ((bytes = send(sokt, &sendMsg, sizeof(sendMsg) , 0 )) < 0 ) {
	    std::cerr << "send failed, send bytes = " << bytes << std::endl;
	}else {

		cout << "send " + sendMsg << endl;

	}




    //----------------------------------------------------------
    //OpenCV 代码
    //----------------------------------------------------------
 
 
    //namedWindow("slamSignal",1);

   // while ( (char)key != 'q') {

	//接收视频的一帧
/*
        if ((bytes = recv(sokt, iptr, imgSize , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }
        
        

	int slamstate  ;

 
	bool mapsave=false , localization =false,  laser=false  ;

	//slamstate
        if ((bytes = recv(sokt, &slamstate, 4 , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    continue ;
        }


	//mapsave
        if ((bytes = recv(sokt, &mapsave, sizeof (mapsave) , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    continue ;
        }

	//localization
        if ((bytes = recv(sokt, &localization, sizeof (localization) , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    continue ;
        }

	//laser
        if ((bytes = recv(sokt, &laser, sizeof (laser ) , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    continue ;
        }

 
	


	switch (slamstate){
		case -1: cout << "system not ready" << endl; break; 
		case 0: cout << "no image yet" << endl; break; 
		case 1: cout << "not initilized" << endl; break; 
		case 2: cout << "ok" << endl; break; 
		case 3: cout << "lost" << endl; break; 
		default : break ; 

	}



 
	if (localization ){ cout << "---------------------------localization , true " <<endl;}

	if (laser ){ cout << "---------------------------laser , true " <<endl;}

	



	if (mapsave ){ cout << "---------------------------mapsave , true " <<endl;}

	int sendMsg = key ;
	if ((char)key ==  'a' ) { sendMsg = 1;}
	else if ( (char) key ==  's' ) { sendMsg = 2;}
	else if ( (char) key ==  'd' ) { sendMsg = 3;}
	else if ( (char) key ==  'f' ) { sendMsg = 4;}
	else if ( (char) key ==  'g' ) { sendMsg = 5;}

*/

	 
 

    close(sokt);
    return 0;
}	


