package pro.dbro.bart.api;

import pro.dbro.bart.api.xml.BartEtdResponse;
import pro.dbro.bart.api.xml.BartLoadResponse;
import pro.dbro.bart.api.xml.BartRouteResponse;
import pro.dbro.bart.api.xml.BartStationListResponse;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by davidbrodsky on 1/13/15.
 */
public interface BartService {

    @GET("/stn.aspx?cmd=stns")
    Observable<BartStationListResponse> getStations();

    @GET("/stn.aspx?cmd=load")
    Observable<BartLoadResponse> getLegLoad(@Query("ld1") String legCode);

    @GET("/etd.aspx?cmd=etd")
    Observable<BartEtdResponse> getEtdResponse(@Query("orig") String originCode);

    @GET("/sched.aspx?cmd=depart")
    Observable<BartRouteResponse> getRouteResponse(@Query("orig") String originCode,
                                                   @Query("dest") String destCode);
}
