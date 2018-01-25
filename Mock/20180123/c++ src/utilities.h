#ifndef getip
#define getip

#include "opencv2/opencv.hpp"
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

using namespace std;


std::string exec(const char* cmd) {
    char buffer[128];
    std::string result = "";
    FILE* pipe = popen(cmd, "r");
    if (!pipe) throw std::runtime_error("popen() failed!");
    try {
        while (!feof(pipe)) {
            if (fgets(buffer, 128, pipe) != NULL)
                result += buffer;
        }
    } catch (...) {
        pclose(pipe);
        throw;
    }
    pclose(pipe);
    return result;
}





string slamBoardIP () {


 string cmd = "cat SLAMBoardIP.txt" ;

 string res = exec( cmd.c_str() ) ; 

 return res.substr ( 0 , res.size() -1 ) ; 

 //return 127.0.0.1;

}



#endif // SOCKETSERVER_H

