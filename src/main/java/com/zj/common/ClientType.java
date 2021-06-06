package com.zj.common;

/**
 * 
 * @author ZJ
 *
 */
public enum ClientType {

	HTTP(0,"http"),
	WEBSOCKET(1,"websocket"),
	;

    private int type;
    private String info;
    
    private ClientType(int type, String info){
        this.type = type;
        this.info = info;
    }
    
    public int getType(){
        return type;
    }

    public String getInfo(){
        return info;
    }

}
