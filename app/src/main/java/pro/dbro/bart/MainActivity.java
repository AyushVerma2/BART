package pro.dbro.bart;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import pro.dbro.bart.api.BartClient;
import pro.dbro.bart.api.xml.BartApiResponse;
import pro.dbro.bart.api.xml.BartEtdResponse;
import pro.dbro.bart.api.xml.BartScheduleResponse;
import pro.dbro.bart.holdr.Holdr_ActivityMain;
import rx.Observable;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.widget.OnTextChangeEvent;
import rx.android.widget.WidgetObservable;
import rx.schedulers.Schedulers;


public class MainActivity extends Activity implements ResponseRefreshListener {
    private String TAG = getClass().getSimpleName();

    private Holdr_ActivityMain holdr;
    private BartClient client;
    private Subscription subscription;

    private View.OnFocusChangeListener inputFocusListener = (inputTextView, hasFocus) -> {
        if (inputTextView.getTag(R.id.textview_memory) != null &&
            !hasFocus &&
            TextUtils.isEmpty(((TextView) inputTextView).getText())) {
                ((TextView)inputTextView).setText(inputTextView.getTag(R.id.textview_memory).toString());
        }
        else if (hasFocus && !TextUtils.isEmpty(((TextView) inputTextView).getText())) {
            inputTextView.setTag(R.id.textview_memory, ((TextView) inputTextView).getText());
            ((TextView) inputTextView).setText("");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        holdr = new Holdr_ActivityMain(findViewById(R.id.container));
        holdr.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        holdr.recyclerView.setItemAnimator(new DefaultItemAnimator());

        holdr.departureEntry.setOnFocusChangeListener(inputFocusListener);
        holdr.destinationEntry.setOnFocusChangeListener(inputFocusListener);

        holdr.reverse.setOnClickListener(view -> swapInputs());

        setActionBar(holdr.toolbar);

        BartClient.getInstance()
                  .subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(client -> {
                      this.client = client;
                      setupAutocomplete(client);
                      restorePreviousInput();
                  });


        subscription = AppObservable.bindActivity(this,
                Observable.merge(WidgetObservable.text(holdr.departureEntry),
                        WidgetObservable.text(holdr.destinationEntry)))
            .throttleLast(10, TimeUnit.MILLISECONDS)
            .distinctUntilChanged(textChangedEvent -> holdr.departureEntry.getText().hashCode() ^
                                                      holdr.destinationEntry.getText().hashCode())
            .flatMap(onTextChangeEvent -> doRequestForInputs(holdr.departureEntry.getText(),
                                                             holdr.destinationEntry.getText()))
            .retry()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                Log.i(TAG, "onNext " + response.getClass());
                displayResponse(response);
            }, throwable -> Log.i(TAG, throwable.getMessage()));

    }

    private void swapInputs() {
        String departureEntry = holdr.departureEntry.getText().toString();

        holdr.departureEntry.setText(holdr.destinationEntry.getText());
        holdr.destinationEntry.setText(departureEntry);

        ObjectAnimator.ofFloat(holdr.reverse, "rotation", 0f, 180f).start();
        holdr.recyclerView.requestFocus();
    }

    private void restorePreviousInput() {
        SharedPreferences prefs = getSharedPreferences("app", Context.MODE_PRIVATE);
        holdr.departureEntry.setText(prefs.getString("orig", ""));
        holdr.destinationEntry.setText(prefs.getString("dest", ""));
        holdr.departureEntry.dismissDropDown();
        holdr.destinationEntry.dismissDropDown();
    }

    private void saveInput() {
        getSharedPreferences("app", Context.MODE_PRIVATE).edit()
                .putString("orig", holdr.departureEntry.getText().toString())
                .putString("dest", holdr.destinationEntry.getText().toString())
                .apply();
    }

    private void setupAutocomplete(BartClient client) {
        ArrayList<String> stationList = new ArrayList<>();
        stationList.addAll(client.getStationNames());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, stationList);
        holdr.departureEntry.setAdapter(adapter);
        holdr.destinationEntry.setAdapter(adapter);
    }

    private void displayResponse(BartApiResponse response) {
        if (response instanceof BartEtdResponse) {
            if (getCurrentFocus() != holdr.destinationEntry)
                hideSoftKeyboard(holdr.departureEntry);
            BartEtdResponse etdResponse = (BartEtdResponse) response;
            if (etdResponse.getEtds() != null && etdResponse.getEtds().size() != 0) {
                if (holdr.recyclerView.getAdapter() instanceof EtdAdapter) {
                    ((EtdAdapter) holdr.recyclerView.getAdapter()).updateResponse(etdResponse);
                } else {
                    if (holdr.recyclerView.getAdapter() != null) ((TripAdapter) holdr.recyclerView.getAdapter()).destroy();
                    holdr.recyclerView.setAdapter(new EtdAdapter(etdResponse, holdr.recyclerView, MainActivity.this));
                }
            } else
                notifyNoTrips();
        }
        else if (response instanceof BartScheduleResponse) {
            hideSoftKeyboard(holdr.destinationEntry);
            BartScheduleResponse routeResponse = (BartScheduleResponse) response;
            if (routeResponse.getTrips() != null && routeResponse.getTrips().size() != 0) {
                if (holdr.recyclerView.getAdapter() instanceof TripAdapter) {
                    ((TripAdapter) holdr.recyclerView.getAdapter()).updateResponse(routeResponse);
                } else {
                    if (holdr.recyclerView.getAdapter() != null) ((EtdAdapter) holdr.recyclerView.getAdapter()).destroy();
                    holdr.recyclerView.setAdapter(new TripAdapter(routeResponse, holdr.recyclerView, this));
                }
            } else
                notifyNoTrips();
        }
        if (holdr.recyclerView.getAdapter().getItemCount() > 0)
            holdr.recyclerView.smoothScrollToPosition(0);
    }

    private void notifyNoTrips() {
        // TODO
        Toast.makeText(this, "No more trains available tonight", Toast.LENGTH_LONG).show();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public void onStop() {
        super.onStop();
        saveInput();
    }

    public void onDestroy() {
        super.onDestroy();
        subscription.unsubscribe();
    }

    private void hideSoftKeyboard (View view) {
        InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }

    private Observable<? extends BartApiResponse> doRequestForInputs(CharSequence departureInput,
                                                                     CharSequence destinationInput) {

        if (!TextUtils.isEmpty(departureInput)) {
            if (!TextUtils.isEmpty(destinationInput)) {
                return client.getRoute(departureInput.toString(),
                                       destinationInput.toString());
            } else {
                return client.getEtd(departureInput.toString());
            }
        }

        throw new IllegalStateException("No input values");
        //return Observable.error(OnErrorThrowable.from(new IllegalStateException(("No input values"))));
    }

    @Override
    public void refreshRequested(BartApiResponse oldResponse) {
        if (oldResponse instanceof BartEtdResponse) {
            client.getEtd(((BartEtdResponse) oldResponse).getStation().getName())
                  .subscribe(this::displayResponse);
        }
        else if (oldResponse instanceof BartScheduleResponse) {
            client.getRoute(((BartScheduleResponse) oldResponse).getOriginAbbreviation(),
                            ((BartScheduleResponse) oldResponse).getDestinationAbbreviation())
                  .subscribe(this::displayResponse);
        }
    }
}
