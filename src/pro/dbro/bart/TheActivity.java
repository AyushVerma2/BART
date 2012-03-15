package pro.dbro.bart;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import com.thebuzzmedia.sjxp.XMLParser;
import com.thebuzzmedia.sjxp.rule.DefaultRule;
import com.thebuzzmedia.sjxp.rule.IRule;
import com.thebuzzmedia.sjxp.rule.IRule.Type;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.app.AlertDialog;


public class TheActivity extends Activity {
	Context c;
	LinearLayout etdLayout;
	etdResponse etdResponse;
	String currentStation = "dbrk";
	
	private static final String[] STATIONS = new String[] {
		"12th St. Oakland City Center","16th St. Mission (SF)","19th St. Oakland",
		"24th St. Mission (SF)","Ashby (Berkeley)","Balboa Park (SF)","Bay Fair (San Leandro)",
		"Castro Valley","Civic Center (SF)","Coliseum/Oakland Airport","Colma","Concord",
		"Daly City","Downtown Berkeley","Dublin/Pleasanton","El Cerrito del Norte","El Cerrito Plaza",
		"Embarcadero (SF)","Fremont","Fruitvale (Oakland)","Glen Park (SF)","Hayward","Lafayette",
		"Lake Merritt (Oakland)","MacArthur (Oakland)","Millbrae","Montgomery St. (SF)",
		"North Berkeley","North Concord/Martinez","Orinda","Pittsburg/Bay Point","Pleasant Hill",
		"Powell St. (SF)","Richmond","Rockridge (Oakland)","San Bruno","San Francisco Int'l Airport",
		"San Leandro","South Hayward","South San Francisco","Union City","Walnut Creek","West Oakland"
    };
	
	private static final HashMap<String, String> STATION_MAP = new HashMap<String, String>() {
		{
		put("12th St. Oakland City Center", "12th");put("16th St. Mission (SF)", "16th");put("19th St. Oakland", "19th");
		put("24th St. Mission (SF)", "24th");put("Ashby (Berkeley)", "ashb");put("Balboa Park (SF)", "balb");put("Bay Fair (San Leandro)", "bayf");
		put("Castro Valley", "cast");put("Civic Center (SF)", "civc");put("Coliseum/Oakland Airport", "cols");put("Colma", "colm");
		put("Concord", "conc");put("Daly City", "daly");put("Downtown Berkeley", "dbrk");put("Dublin/Pleasanton", "dubl");
		put("El Cerrito del Norte", "deln");put("El Cerrito Plaza", "plza");put("Embarcadero (SF)", "embr");put("Fremont", "frmt");
		put("Fruitvale (Oakland)", "ftvl");put("Glen Park (SF)", "glen");put("Hayward", "hayw");put("Lafayette", "lafy");
		put("Lake Merritt (Oakland)", "lake");put("MacArthur (Oakland)", "mcar");put("Millbrae", "mlbr");put("Montgomery St. (SF)", "mont");
		put("North Berkeley", "nbrk");put("North Concord/Martinez", "ncon");put("Orinda", "orin");put("Pittsburg/Bay Point", "pitt");
		put("Pleasant Hill", "phil");put("Powell St. (SF)", "powl");put("Richmond", "rich");put("Rockridge (Oakland)", "rock");
		put("San Bruno", "sbrn");put("San Francisco Int'l Airport", "sfia");put("San Leandro", "sanl");put("South Hayward", "shay");
		put("South San Francisco", "ssan");put("Union City", "ucty");put("Walnut Creek", "wcrk");put("West Oakland", "woak");
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        etdLayout = (LinearLayout) findViewById(R.id.etdLayout);
        c = this;
        //new RequestTask(this).execute("http://api.bart.gov/api/etd.aspx?cmd=etd&orig=dbrk&key=MW9S-E7SL-26DU-VV8V");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, STATIONS);
        AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.tv);
        textView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View arg1, int position,
					long arg3) {
			
				currentStation = STATION_MAP.get(parent.getItemAtPosition(position).toString());
				Log.v("Station",currentStation);
				
				new RequestTask((Activity)c).execute("http://api.bart.gov/api/etd.aspx?cmd=etd&orig="+currentStation+"&key=MW9S-E7SL-26DU-VV8V");
			}
        });
        
        textView.setAdapter(adapter);
    }
    
    public void parseBart(String response){
    	
    	new BartParser(this).execute(response);
    }
    
    public void updateUI(etdResponse response){
    	TextView tv = (TextView) findViewById(R.id.tv);
    	etdResponse = response;
    	tv.setText(response.toString());
    	fillTable();
    }
   
    public void fillTable(){
		etdLayout.removeAllViews();
		for(int x=0;x<etdResponse.etds.size();x++){
			TableRow tr = new TableRow(c);
			TextView bullet = (TextView) View.inflate(c, R.layout.tabletext, null);
			//bullet.setWidth(200);
			bullet.setTextSize(20);
			bullet.setText(((etd)etdResponse.etds.get(x)).destination);
			TextView text = (TextView) View.inflate(c, R.layout.tabletext, null);
			text.setText(String.valueOf(((etd)etdResponse.etds.get(x)).minutesToArrival));
			text.setSingleLine(false);
			text.setTextSize(36);
			//text.setWidth(120);
			tr.addView(bullet);
			tr.addView(text);
			etdLayout.addView(tr);
		}
		//scrolly.scrollTo(0, 0);
	}
    
}