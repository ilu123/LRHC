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
#include <math.h> 

using namespace std;
using namespace cv;


char*       serverIP;
int         serverPort=4104;  // 导航



float orix, oriy ;                                 
float reso = 0.025 ; 
cv::Mat img;  // image container
cv::Mat markLayer ;  // 目标点图层
cv::Mat poseLayer ;  // 当前位置图层
cv::Mat pathLayer ;   // 路径图层


bool send_flag = false ;
float x , y ;
float angle ; 
int ix= -1 ,iy=-1 ;


int sokt;

 

cv::Point landMark (0,0) ;  // 目标地点的像素坐标

cv::Point pose (0,0) ;  // 当前位置的像素坐标

std::vector< std::pair < float , float >  > vec;   



bool drawing = false; // true if mouse is pressed
 




bool sendSignal( int sokt , int signal ) {

	int bytes = 0 ;


        if ((bytes = send(sokt,  &signal,  4 , 0)) == -1 ){

                std::cout << "send error !" << endl;
                return false ;

        }

	//cout << "sent bytes: " << bytes << " signal: " << signal <<endl;
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


bool sendFloat( int sokt , float signal  ) {

	int bytes = 0 ;


        if ((bytes = send(sokt,  &signal,  sizeof ( signal ) , 0)) == -1 ){

                std::cout << "send error !" << endl;
               	return false ;
        }

	return  true ; 
}


bool printAck ( int sokt ) {

	int bytes = 0 ; 

	char resuBuff [1024] ; 
        if ((bytes = recv(sokt, resuBuff , 1024 ,  0)) == -1 ){

                std::cout << "recv error !" << endl;
                
		return false ;
        } 

	//cout << " ack : " << resuBuff << endl;

	return true;



}


void click( int event, int px, int py,  int flags , void* params ){


        if (event == cv::EVENT_LBUTTONDOWN ){   // 设置目标点位置


	        drawing = true ;
        	ix = px;
		iy = py ;

			

		markLayer.setTo( cv::Scalar (0)) ; 

		int bytes = 0 ;
		//cv::circle ( img ,   cv::Point ( px , py )  , 6 , cv::Scalar(175) , -1 , CV_AA  ) ;

                


		 
	} 
    	else if ( event == cv::EVENT_MOUSEMOVE ) {  // 设置方向， 鼠标释放后，方向确定

		 


        	if  (drawing) {

			markLayer.setTo( cv::Scalar (0)) ; 
 

			cv::circle ( markLayer ,  cv::Point (ix,iy)  , 4, cv::Scalar(255,0,0) , -1 , CV_AA  ) ;

			cv::line(markLayer, cv::Point(ix,iy), cv::Point(px,py), (0,0,167),2) ; 

 

		}

	}else if (event == cv::EVENT_LBUTTONUP ) {

		drawing = false ;

		
		float pi = 3.1415 ; 


		angle = - atan2 (    float  (py-iy)  , float(px-ix)  ) ;

 


		x= ix * reso + orix ;
                y= oriy + ( img.rows - iy ) * reso ;
		send_flag = true; 
		landMark.x = ix ;
		landMark.y = iy ; 

	}

}



void showImg () {



	while (true) { 

		cv::Mat combine = img -	 markLayer - poseLayer - pathLayer  ; 

 


		poseLayer.setTo( cv::Scalar (0)) ; 
		cv::circle ( poseLayer ,  pose  , 6 , cv::Scalar(0,255,0) , -1 , CV_AA  ) ;


 

		for ( unsigned int i = 0 ; i + 1  < vec.size() ; i+=2  ) {

			float x = vec[i].first;
			float y = vec[i].second;

			int px = (int) ( (x-orix)/reso ) ;
			int py = (int) (img.rows - (y-oriy)/reso ) ; 				



			pathLayer.setTo( cv::Scalar (0)) ; 
			cv::circle ( combine ,   cv::Point (px,py)  , 1 , cv::Scalar(175) , -1 , CV_AA  ) ;

		}	




		cv::imshow ( "image" , combine  ) ; 

 
		

		int key = cv::waitKey (20) ;

		if ( key == 'q') break; 

	}
	
}

	 
 


void fakeRVIZ ( ) {

	int bytes = 0 ;
	


	
	string cmdx= "cat origin.txt | awk '{ print $1}'" ;
	string cmdy= "cat origin.txt | awk '{ print $2}'" ;
	string rx =  exec(cmdx.c_str()); 				
	string ry =  exec(cmdy.c_str()); 
	orix = stof ( rx );
	oriy = stof ( ry );
	
	



	while ( true ) { 

		

		//




		// send goal 
		
		if ( send_flag ){


			cv::circle ( markLayer ,  landMark  , 8 , cv::Scalar(255,0,0) , -1 , CV_AA  ) ;			

			send_flag = false ; 

			if ( sendSignal ( sokt , 1005  ) )  { 

				cout << "send succeed" << endl ;

				if (!sendFloat(sokt , x)) return  ;
				if (!sendFloat(sokt , y)) return  ;
				if (!sendFloat(sokt , angle)) return  ;


			}    else { 

				cout << "send fail" << endl ;
			}

		}

		// get pose
		if ( sendSignal ( sokt , 1007  ) )  { 

			float pose_x = recvFloat ( sokt   ) ;

			
			if ( x < -10000) {  cout << "recv float error " << endl ;  return ; }			


			float pose_y = recvFloat ( sokt   ) ;
			if ( y < -10000) {  cout << "recv float error " << endl ;  return ; }	

			cout << " psoe: x, y " << pose_x << " " << pose_y << endl;		


			float z = recvFloat ( sokt   ) ;

		
		        int px = (int) ( (pose_x-orix)/reso ) ; 
		        int py = (int) (img.rows - (pose_y-oriy)/reso ) ; 

			cout << " psoe: px, py " << px << " " << py << endl;

			

			//poseLayer.setTo( cv::Scalar (0)) ; 
			//cv::circle ( markLayer ,  cv::Point (px,py)  , 6 , cv::Scalar(0,255,0) , -1 , CV_AA  ) ;


			pose.x = px ;
			pose.y = py ;	

	

		} else { return ;}



		// get path 
		if ( sendSignal ( sokt , 1008  ) )  { 

			
			int size ;

			if ((bytes = recv(sokt,  &size,  sizeof (size) , 0)) <= 0 ){

				std::cout << "recv error !" << endl;
				return  ;

			}

			

 

			//cout << " point size : " << size << endl;  
			
			if ( size >0 ){


				 // red pointcloud
				vec.resize (size) ; 		

				//float f = 0.0 ;

				if ((bytes = recv(sokt, &(*vec.begin()), size * 8  , MSG_WAITALL)) <= 0) {
				    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
				    return ;
				}

			

						
			}

		} else { return ; }








		cv::circle ( markLayer ,   landMark  , 6 , cv::Scalar(0,0,255) , -1 , CV_AA  ) ;
 


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



	cv::namedWindow("image");
	cv::setMouseCallback("image", click);

	//read Map 
	img = cv::imread ( "theMap.pgm" ) ; 


	img.copyTo ( markLayer ) ;

	img.copyTo ( poseLayer ) ;

	img.copyTo ( pathLayer ) ;

	markLayer.setTo( cv::Scalar (0)) ; 

	poseLayer.setTo( cv::Scalar (0)) ; 

	pathLayer.setTo( cv::Scalar (0)) ; 



	std::thread it  ( fakeRVIZ  );   // 路径，当前位置 接受函数，  后台异步接收
	it.detach() ; 


	showImg();   // 显示界面


    	close(sokt);

	delete [] serverIP ;

    	return 0;

}	
