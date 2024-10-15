package com.gw.cp_account.http;

import com.gw.lib_http.HttpResponse;

public class PhoneCodeResponse extends HttpResponse {
    private int id;
    private String vKey;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return vKey;
    }

    public void setKey(String vKey) {
        this.vKey = vKey;
    }

    @Override
    public String toString() {
        return "PhoneCodeResult{" +
                "id=" + id +
                ", vKey='" + vKey + '\'' +
                '}';
    }
}
