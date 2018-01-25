/**
 * HCLR SLAM Product interface demo
 */
#include <cmath>

#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <sstream>
#include "cmdline.h"
#include "utilities.h"
using namespace std;
using namespace cv;


char*       serverIP= "10.42.0.1";
int         serverPort=4103;  // this port is for psa 



float orix, oriy ;                                 
float reso = 0.025 ; 
cv::Mat img;  // image container


int sokt;


std::vector < std::pair <  cv::Point  , bool > >  landmarks ; 

bool sendSignal( int sokt , int signal ) {

	int bytes = 0 ;


        if ((bytes = send(sokt,  &signal,  4 , 0)) == -1 ){

                std::cout << "send error !" << endl;
                return false ;

        }

	cout << "sent bytes: " << bytes << " signal: " << signal <<endl;
	return true ; 
}


int recvSignal( int sokt  ) {

	int bytes = 0 ;

	int signal = -1 ;

        if ((bytes = recv(sokt,  &signal,  4 , 0)) <= 0 ){

                std::cout << "recv error !" << endl;
                return -1 ;

        }

	return  signal ; 
}



float recvFloat( int sokt  ) {

	int bytes = 0 ;

	float signal = -1000000 ;

        if ((bytes = recv(sokt,  &signal,  sizeof ( signal ) , 0)) <= 0 ){

                std::cout << "recv error !" << endl;
               	

        }

	return  signal ; 
}




bool printAck ( int sokt ) {

	int bytes = 0 ; 

	char resuBuff [1024] ; 
        if ((bytes = recv(sokt, resuBuff , 1024 ,  0)) == -1 ){

                std::cout << "recv error !" << endl;
                
		return false ;
        } 

	cout << " ack : " << string(resuBuff).substr(0,bytes) << endl;

	return true;



}


void click( int event, int px, int py,  int flags , void* params ){

        if (event == cv::EVENT_LBUTTONDOWN ){

		//cv::circle ( img ,   cv::Point ( px , py )  , 6 , cv::Scalar(175) , -1 , CV_AA  ) ;

                float x= px * reso + orix ;
                float y= oriy + ( img.rows - py ) * reso ;

		if ( sendSignal ( sokt , 1080  ) ) ; 

		cout << " l pressed" << endl;

		landmarks.push_back ( std::make_pair (  cv::Point ( px , py ) , true )) ;

	} 

	/*

	else if  (  event == cv::EVENT_RBUTTONDOWN ) {


		cout << " r pressed" << endl ;

		for (  unsigned int i = 0 ; i < landmarks.size()  ; i ++ ) {

			cout << "i = " << i << endl;  

			if ( landmarks[i].second  ) { 

				cv::Point p = landmarks[i].first ; 

				cout << "" << px  << " , " << p.x << " , " << py << " , " << p.y << endl;  

				int prod =     (px- p.x)* (px- p.x)  +  (py- p.y)* (py- p.y)    ;
				cout << "product: "<< prod << endl;   
				double  distance = sqrt (  (float)prod ) ; 
				
				cout << "distance" << endl ; 
				
				if (  prod < 1600 ) { 
					landmarks[i].second = false ;
				} 
			}

		}


	} */

}


void fakeRVIZ ( ) {


	cv::namedWindow("image");
	cv::setMouseCallback("image", click);


	cv::Mat markLayer ;

	img.copyTo ( markLayer ) ;


	while ( true ) { 

		markLayer.setTo( cv::Scalar (0)) ; 

		for ( unsigned int i = 0 ; i < landmarks.size()  ; i ++ ) {

                        if ( landmarks[i].second  ) {

                        	cv::Point p = landmarks[i].first ;
				
				cv::circle ( img ,   cv::Point ( p.x , p.y )  , 6 , cv::Scalar(175) , -1 , CV_AA  ) ;

			}

		}
		

		cv::imshow ( "image" , img  ) ; 

		int key = cv::waitKey (1) ;

		if ( key == 'q') break; 
	}

}


int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------i




    string str = slamBoardIP();


    cout << " server IP:  " << str << endl;

   serverIP = new char [ str.size() + 1  ] ; 

    serverIP[str.size()]= '\0' ; 

    std::copy(str.begin(), str.end(), serverIP) ;







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


  cmdline::parser a;
  a.add("bringup" ,  'b' ,   " turtlebot bring up"  );   // 取消了， 不再使用该命令
  a.add("reset_system" ,  'u' ,   "reset handle socket "  );   // 重置系统
  
  a.add("teleop" ,    't' ,   " start / stop teleop"  );
  
  a.add("shutdown" ,  'd' ,   " shutdown slam borad"  );
  a.add("reboot" ,    'r' ,   " reboot slam board"  );
  a.add("stopMission" ,   'm' ,   " stop current navigation mision "  );

  a.add("slam" ,    's' ,   " start / stop slam mode"  );

  a.add("getMap" ,  'g' ,   " get current map "  );

  a.add("navi" ,  'n' ,   " start Navigation "  );

  a.add("closeNavi" ,  'c' ,   " close Navigation "  );  

  a.add("version" ,  'v' ,   " see slam board version "  ) ; // 查看版本号 

  //a.add("reposition" ,  'p' ,   "assign position to robot"  );

  //a.add("bringup" ,  'b' ,   " turtlebot bring up"  );
  //a.add<int>("port", 'p', "port number", false, 80, cmdline::range(1, 65535));
  //a.add<string>("type", 't', "protocol type", false, "http", cmdline::oneof<string>("http", "https", "ssh", "ftp"));
  a.add("help", 0, "print this message");
  //a.footer("filename ...");
  //a.set_program_name("test");

  bool ok=a.parse(argc, argv);

  if (argc==1 || a.exist("help")){
    cerr<<a.usage();
    return 0;
  }
  
  if (!ok){
    cerr<<a.error()<<endl<<a.usage();
    return 0;
  }



    int bytes = 0 ;

    int signal =0 ; // kill a process with its Name      


    if ( a.exist ( "reset_system" ) ) {
	
	sendSignal ( sokt, 1078 );

	printAck(sokt) ;

  }else if ( a.exist ( "teleop" ) ) {
	
	sendSignal ( sokt, 1079 );

	printAck(sokt) ;

  }else if ( a.exist ( "shutdown" ) ) {
	
	sendSignal ( sokt, 1000 );

	printAck(sokt) ;

  }else if ( a.exist ( "reboot" ) ) {

	sendSignal ( sokt , 1001) ;
	printAck(sokt) ;

  } else if ( a.exist ("stopMission") ) {
	
	sendSignal (  sokt , 1002 ) ; 
	printAck(sokt) ;

  }else if ( a.exist ( "slam" ) ) {
 
      
	sendSignal (sokt , 1003 ) ;

	string mapName = argv[2] ; 
	cout << "map name: " << mapName << endl;

        if ((bytes = send(sokt, mapName.c_str(), mapName.size(), 0)) == -1 ){

                std::cout << "send error !" << endl;

                return 0;

        }

	 printAck(sokt) ;

  } else if ( a.exist ( "getMap" ) )  {

	if ( sendSignal ( sokt , 1006)  ) {

		int i = recvSignal ( sokt  ) ;

		if (i != -1) {
			if (i ==0) {
			
				cout << "getMap fail" <<endl ; 
			} else if ( i ==1 ) {
			
				orix = recvFloat ( sokt   ) ;
				if ( orix < -10000) {  cout << "recv float error " << endl ;  return 0; }			


				oriy = recvFloat ( sokt   ) ;
				if ( oriy < -10000) {  cout << "recv float error " << endl ;  return 0; }			

				int width = recvSignal ( sokt) ; 
				if (width == -1) { cout << "recv  error " << endl ;  return 0; } 				

	
				int height = recvSignal ( sokt) ; 
				if (height == -1) { cout << "recv  error " << endl ;  return 0; } 	

				cout << " orix: "<<orix << " y: " <<oriy << " w:" << width << " h: "<< height << endl;



                                img = cv::Mat::zeros( height ,  width , CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char  
                                int imgSize = img.total() * img.elemSize();
                                uchar *iptr = img.data;

        //                      cout << "imgSize " << imgSize<< endl;
                                if ((bytes = recv(sokt, iptr, imgSize , MSG_WAITALL)) <=0) {
                                     std::cerr << "recv failed, received bytes = " << bytes << std::endl;
                                     
                                     return 0 ;
                                 }
			
                                        
				stringstream ss;                                   
				ss << orix;


				stringstream ss1;                                   
				ss1 << oriy;

				cv::imwrite ( "theMap.pgm" , img ) ; 
				string cmd = "echo " + ss.str() + " " + ss1.str() + " > origin.txt "  ;
				
 				system ( cmd.c_str() ) ;


				//fakeRVIZ( ) ;   
				cv::imshow ( "running map " ,  img  ); 

				cv::waitKey () ;


				

				  			
				

			}

		}
	}

  }else if ( a.exist ( "navi" ) ) {


        sendSignal (sokt , 1013 ) ;

        string mapName = argv[2] ;
        cout << "map name: " << mapName << endl;

        if ((bytes = send(sokt, mapName.c_str(), mapName.size(), 0)) == -1 ){

                std::cout << "send error !" << endl;

                return 0;

        }

        // printAck(sokt) ;

  }else if ( a.exist ( "closeNavi" ) ) {


        sendSignal (sokt , 1077 ) ;

        // printAck(sokt) ;

  }else if  ( a.exist ( "version") ) {

	sendSignal ( sokt , 1099 ) ;

	printAck(sokt) ;


  }

    	close(sokt);

	delete  [] serverIP ; 

    	return 0;

}	
