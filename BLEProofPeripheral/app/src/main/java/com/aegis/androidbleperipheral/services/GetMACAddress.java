package com.aegis.androidbleperipheral.services;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class GetMACAddress {
    public String getMacAddress(){
        try{
            List<NetworkInterface> networkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());

            StringBuilder stringMac = new StringBuilder();

            for(NetworkInterface networkInterface : networkInterfaceList)
            {
                if(networkInterface.getName().equalsIgnoreCase("wlon0")){
                    for(int i = 0 ;i <networkInterface.getHardwareAddress().length; i++){
                        String stringMacByte = Integer.toHexString(networkInterface.getHardwareAddress()[i]& 0xFF);

                        if(stringMacByte.length() == 1)
                        {
                            stringMacByte = "0" +stringMacByte;
                        }

                        stringMac.append(stringMacByte.toUpperCase()).append(":");
                    }
                    break;
                }

            }
            return stringMac.toString();
        }catch (SocketException e)
        {
            e.printStackTrace();
        }

        return  "0";
    }
}
