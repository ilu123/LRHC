/**
 * 显示点云的样例代码
 */

#include "opencv2/opencv.hpp"
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <vector>

using namespace cv;
using namespace std;

char*       serverIP;
int         serverPort=4097;  // 该端口用来显示点云

int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------



    string str = argv [1] ;

    serverIP = new char [ str.size() + 1  ] ;

    serverIP[str.size()]= '\0' ;

    std::copy(str.begin(), str.end(), serverIP) ;


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




    //----------------------------------------------------------
    //OpenCV Code
    //----------------------------------------------------------



std::vector< std::pair< int , int> > vt;
vt.push_back ( std::make_pair (1,1) ) ;
cout << sizeof ( vt[0] ) << endl;

int bytes = 0;

Mat img = Mat( 600 , 600 , CV_8UC3 , cv::Scalar(255,255,255) );

int key ;

int reconnect = 0 ;
while (true) {


	if ( 	    reconnect  ){
	
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

std::vector< std::pair< int , int> > vec; // red pointcloud  红色点云vector
std::vector< std::pair< int , int> > vec1;  // black pointcloud 黑色点云vector
std::vector< std::pair< int , int> > vec2;  // current pose   轨迹vector
int64_t sz;  // vector的长度容器

int64_t sz1;  // vector的长度容器

int64_t sz2;  // vector的长度容器

//接收点云长度


	
/*
	if ((bytes = recv(sokt, &sz, sizeof(sz) , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;


	    reconnect = 1 ;		

	    continue ; 

	}

	//cout<< "red: " << sz << endl;


	

	//更改vector尺寸
	try{

		vec.resize( sz );

	} catch ( const std::exception &e) {

		cout << "caught exception on red" << endl;

		close(sokt);
		reconnect = 1 ; 
		continue ; 
		
	}
	//cout << " resize red dot vector "<< endl;
	//接收红色点云
	if ((bytes = recv(sokt, &(*vec.begin()), sz * sizeof( vt[0] )  , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;


	    reconnect = 1 ;
	    continue ; 
	}

*/

	int state ; 







	//接收slam state
	if ((bytes = recv(sokt, &state, sizeof(state) , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;

	    reconnect = 1 ;

	    continue ; 
	}


	switch (state){
		case -1: cout << "system not ready" << endl; break; 
		case 0: cout << "no image yet" << endl; break; 
		case 1: cout << "not initilized" << endl; break; 
		case 2: cout << "ok" << endl; break; 
		case 3: cout << "lost" << endl; break; 
		default : break ; 

	}

	








	//接收尺寸
	if ((bytes = recv(sokt, &sz1, sizeof(sz1) , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;

	    reconnect = 1 ;

	    continue ; 
	}




	cout<< "black: " << sz1 << endl;
	//更改vector尺寸
	try{
		vec1.resize( sz1 );

	} catch (const std::exception &e ) {

		cout << "caught exception on black" << endl;
		close(sokt);
		reconnect = 1 ; 
		continue ; 
		//cout << "black big" << endl;				
		
	}


	//cout << " resize black dot vector "<< endl;

	//接收黑色点云
	if ((bytes = recv(sokt, &(*vec1.begin()), sz1 * sizeof( vt[0] )  , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    reconnect = 1 ;
	    continue ; 
	}

	reconnect = 0 ;


	int pointCnt = vec1[ 0 ].first ;	

	string str = " map points number : " + to_string ( pointCnt )  ; 


	putText(img, str, Point(100, 100), FONT_HERSHEY_PLAIN, 2,  Scalar(0,0,255,255));
	


	//接收尺寸
	if ((bytes = recv(sokt, &sz2, sizeof(sz2) , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	} 
	//更改vector大小

	cout << "kframe size " << sz2 << endl;

	//更改vector尺寸
	try{
		vec2.resize( sz2 );	
	} catch (const std::exception &e ) {

		cout << "caught exception on trjectory" << endl;
		close(sokt);
		reconnect = 1 ; 
		continue ; 
	}


	//cout << " resize kfram vector "<< endl;
	//接收轨迹点云
	if ((bytes = recv(sokt, &(*vec2.begin()), sz2 * 8  , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    reconnect = 1 ;
	    continue ; 
	}





	
	//画红色点
	for (int i = 0 ; i < vec.size () ; i ++ ) {
		cv::circle( img , cv::Point (vec[i].first, vec[i].second ) , 1, cv::Scalar(0,0,255) ,  -1 );

	}
	//画黑色点
	for (int i = 0 ; i < vec1.size () ; i ++ ) {
		cv::circle( img , cv::Point (vec1[i].first, vec1[i].second ) , 1, cv::Scalar(0,0,0) ,  -1 );
	}

	



	//画轨迹
	for (int i = 0 ; i < vec2.size () ; i ++ ) {
		cv::circle( img , cv::Point (vec2[i].first, vec2[i].second ) , 2 , cv::Scalar(0,255,0) ,  1 ,  CV_AA );
	}

	cv::circle( img , cv::Point (vec2[0].first, vec2[0].second ) , 5 , cv::Scalar(255,0,0) ,  -1 ,  CV_AA );





	bool mapsave=false   ;

 

	//mapsave
        if ((bytes = recv(sokt, &mapsave, sizeof (mapsave) , MSG_WAITALL)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	    continue ;
        }

	if (mapsave ) {

		cout << "map save finish !!!!!!!" << endl;

	}





	//显示画布
	cv::imshow("RGBD Camera", img);

 
 

if (key = cv::waitKey(10) ==  113) break;

//重置画布
img.setTo(cv::Scalar(255,255,255));


}



close(sokt);

return 0;
}
