package com.howfun.music.control;

interface IMusicService
{ 
    void processPlayPauseRequest();
    
    void processPlayNowRequest();
    
    int getPosition();
    
    void setPosition(int pos);
    
    int getState();
    
    void stop();
    
    String getCurDisplayStr();
    
    Uri getAlbumUri();
} 
