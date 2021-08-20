package com.wayneyong.poibrowser.network;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.wayneyong.poibrowser.network.place.PlaceResult;

import java.util.List;

public class PlaceResponse {

    @SerializedName("html_attributions")
    @Expose
    private List<Object> htmlAttributions = null;

    @SerializedName("result")
    @Expose
    private PlaceResult result;

    @SerializedName("status")
    @Expose
    private String status;

    public List<Object> getHtmlAttributions() {
        return htmlAttributions;
    }

    public void setHtmlAttributions(List<Object> htmlAttributions) {
        this.htmlAttributions = htmlAttributions;
    }

    public PlaceResult getResult() {
        return result;
    }

    public void setResult(PlaceResult result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
