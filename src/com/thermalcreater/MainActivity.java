package com.thermalcreater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
private Button file_igc;
private TextView text_igc,text_thermal;
private EditText edit_factor;
private View textEntryView=null;
private final static int IGCFILE=0;
String igcpath,igcname,starttime,endtime,currenttime,
lat=null,lathems=null,firstlathems=null,lastlathems=null,
lon=null,lonhems=null,firstlonhems=null,lastlonhems=null;	
int count=0,totalwp=0,altupcount=0,altdowncount=0,tercount=0,maxalt=0,thermalfactor=30;
double dlat,dlon,terstartlat,terstartlon,terendlat,terendlon,finalLat,finalLon,firstlat,firstlon,lastlat,lastlon,alt,oldalt=0;
ArrayList<String> igcdetail = new ArrayList<String>();
boolean terstart=false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		file_igc = (Button) findViewById(R.id.file_igc);
		text_igc = (TextView) findViewById(R.id.text_igc);
		text_thermal= (TextView) findViewById(R.id.text_thermal);		
		file_igc.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {				
				startMapFilePicker();
			}
		});				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	private void startMapFilePicker() {		
		FilePicker.setFileDisplayFilter(new FilterByFileExtension(".igc;.IGC"));
		startActivityForResult(new Intent(this, FilePicker.class), IGCFILE);		
	}
	@Override
		protected void onActivityResult(int requestCode, int resultCode, final Intent data) {		
			super.onActivityResult(requestCode, resultCode, data);		
			switch (requestCode) {					
			case IGCFILE:					
				if (resultCode == Activity.RESULT_OK) {	
					if (data != null && data.getStringExtra(FilePicker.SELECTED_FILE) != null) {	
						LayoutInflater factory = LayoutInflater.from(this);
						textEntryView = factory.inflate(
								R.layout.dialog_enter, null);			
						final EditText inputurl = (EditText) textEntryView
								.findViewById(R.id.t_factor);			
						inputurl.setText("30", TextView.BufferType.EDITABLE);				
						final AlertDialog.Builder alerturl = new AlertDialog.Builder(this);
						alerturl.setTitle("Enter the thermal factor - Optimum=20")
								.setView(textEntryView)
								.setPositiveButton("Create WP File",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog,
													int whichButton) {	
												try{
												thermalfactor=Integer.parseInt(inputurl.getText().toString());	
												runCreater(data);
												}catch(Exception e){	
													Toast.makeText(getBaseContext(), "Wrong Input Type", Toast.LENGTH_SHORT).show();
												}																			
											}
										})
								.setNegativeButton("Cancel",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog,
													int whichButton) {
											}
										});
						alerturl.show();		
					}	
				}
				break;	
			}
		}
	public void runCreater(Intent data)
	{
		new DownloadFileAsync(this).execute(data.getStringExtra(FilePicker.SELECTED_FILE));		
	}
	 public static double getDistance(double lat1, double lat2,double lon1,double lon2) {		   
		    double dLat = Math.toRadians(lat2-lat1);
		    double dLon = Math.toRadians(lon2-lon1);
		    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		    Math.sin(dLon/2) * Math.sin(dLon/2);
		    double c = 2 * Math.asin(Math.sqrt(a));
		    return 6366000 * c;
		}
	
	 public class DownloadFileAsync extends AsyncTask<String, String, String> {
			public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
			private ProgressDialog mProgressDialog;
			
			public DownloadFileAsync(Context context) 
			{				 
			     mProgressDialog = new ProgressDialog(context);
			     mProgressDialog.setMessage("Creating Thermal Waypoints...Please Wait");
			     mProgressDialog.setIndeterminate(false);
			     mProgressDialog.setMax(0);
			     mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			     mProgressDialog.setCancelable(true);				    
			}
			@Override
			protected void onPreExecute() {
			    super.onPreExecute();			   
			    mProgressDialog.show();
			    count=0;
			    totalwp=0;
			    maxalt=0;
			    tercount=0;
			    igcdetail.clear();
			}
			@Override
			protected String doInBackground(String... aurl) {			
				String path=igcpath=aurl[0];						
				File file = new File(path);
				igcname=file.getName();
				if (file.exists()) 
				{
					try {
		    		    BufferedReader br = new BufferedReader(new FileReader(file));
		    		    String line;
		    		    while ((line = br.readLine()) != null) {
		    		    	if(line.startsWith("B"))	    		    		
		    		    	{	 
		    		    		 count++;
		    		    	}
		    		    }
		    		}
		    		catch (IOException e) {	    		   
		    		}	
					mProgressDialog.setMax(count);		
					totalwp=count;
					count=0;
					file = new File(path);						
					try {						
			    		   BufferedReader br = new BufferedReader(new FileReader(file));
			    		    String line;			    		 
			    		    while ((line = br.readLine()) != null) {
			    		    try{
			    		    	if(line.startsWith("B"))
			    		    	{			    		    		 
			    		    		lat=line.substring(7, 14);
			    		    		lathems=line.substring(14, 15);
			    		    		lon=line.substring(15, 23);
			    		    		lonhems=line.substring(23, 24);		
			    		    		dlat=DDMMmmmToDecimalLat(lat,lathems);
			    		    		dlon=DDMMmmmToDecimalLon(lon,lonhems);			    		    		
			    		    		String strtrckalt=line.substring(line.indexOf("A")+6, line.indexOf("A")+11);	
			    		    		alt=Integer.parseInt(strtrckalt);
			    		    		if(alt>maxalt)
			    		    			maxalt=(int)alt;
			    		    		if(alt>oldalt)
			    		    		{
			    		    			altupcount++;
			    		    			if(altupcount>=thermalfactor && !terstart)
			    		    			 {
			    		    				 terstart=true;
			    		    				 terstartlat=dlat;
			    		    				 terstartlon=dlon;
			    		    				 altdowncount=0;
			    		    			 }
			    		    		}else
			    		    		{
			    		    			altdowncount++;
			    		    			if(altdowncount>=(int)thermalfactor/2 && terstart)
			    		    			{				    		    			
				    		    			terendlat=dlat;
			    		    				terendlon=dlon;
			    		    				tercount++;
			    		    				//SetMitPosition();
			    		    				String wp="<wpt lat=\""+terstartlat+"\" lon=\""+terstartlon+"\">";	    		    			    			
		    		    			    	 igcdetail.add(wp);	
		    		    			    	 wp="<name>WP"+tercount+"</name>";	 
		    		    			    	 igcdetail.add(wp);	
		    		    			    	 wp="</wpt>";
		    		    			    	 igcdetail.add(wp);			    		    			    	 
		    		    			    	 altupcount=0;
		    		    			    	 terstart=false;
			    		    			}
			    		    		}
			    		    		oldalt=alt;
	    		    			    if(count==0)
	    		    			    {
	    		    			    	starttime=line.substring(1,3) + ":" +
					    		    			 line.substring(3,5) + ":" +
					    		    			 line.substring(5,7);	
	    		    			    	firstlat=dlat;
	    		    			    	firstlathems=lathems;
	    		    			    	firstlon=dlon;
	    		    			    	firstlonhems=lonhems;
	    		    			    	igcdetail.add("<?xml version=\""+"1.0\""+"?>\n");
	    		    			    	igcdetail.add("<gpx version="+"\"0.1\" creator=\""+"Thermal Creater 1.0 - http://turkaybiliyor.wix.com/projects\""+">");
	    		    			    }else if(count==totalwp-1)
	    		    			    {
	    		    			    	endtime=line.substring(1,3) + ":" +
					    		    			 line.substring(3,5) + ":" +
					    		    			 line.substring(5,7);	
	    		    			    	lastlat=dlat;
	    		    			    	lastlathems=lathems;
	    		    			    	lastlon=dlon;
	    		    			    	lastlonhems=lonhems;	    		    			    	
	    		    			    }    		    			    		
	    		    			    count++;	    				
	    		    			    mProgressDialog.setProgress(count);	
			    		    	}		    		    	
			    		    }catch(Exception e){			    		    	
			    		    }	
			    		   }
			    		   br.close();	    		   
			    		}
			    		catch (IOException e) {			    			
			    		}	
					
				}
			    return null;			 
			}				
			protected void onProgressUpdate(String... progress) {
			    mProgressDialog.setProgress(Integer.parseInt(progress[0]));
			}		
			@Override
			protected void onPostExecute(String unused) {	
				    igcdetail.add("</gpx>");		
				    String sFileName=igcname.replace(".igc", "")+"_Waypoints.gpx";
					try {
						File root = new File(Environment.getExternalStorageDirectory(),
								"Waypoints");
						if (!root.exists()) {
							root.mkdirs();
						}
						File igcfile = new File(root, sFileName);	
						igcfile.setWritable(true);
						 if(igcfile.exists())
		        	       	{
		        	       		boolean deleted = igcfile.delete();	        	       		
		        	    	}						
						FileWriter writer = new FileWriter(igcfile);
						for (String str : igcdetail) {
							writer.write(str+"\r\n");							
						}
						writer.flush();
						writer.close();
						igcdetail.clear();
						Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						Uri uri = Uri.fromFile(igcfile);
						intent.setData(uri);
						sendBroadcast(intent);	
						DecimalFormat df = new DecimalFormat("0.0");			
						String maxdistance=df.format(getDistance(firstlat,lastlat,firstlon,lastlon)	/1000);
						text_igc.setText("Thermal Waypoints created on:\n"+ igcfile.getAbsolutePath() +"\n"
								+"StartTime"+starttime + "\n"
								+"EndTime"+endtime + "\n"
								+"Max Distance"+ "\n"
								+ maxdistance+ " km"+ "\n"	
								+"Max Altitude"+ "\n"
								+ maxalt+ " m"+ "\n"
								+"Total Points"+ "\n"
								+ String.valueOf(totalwp)															
								);
						text_thermal.setText("Total Thermal Points"+ "\n"
								+ String.valueOf(tercount)	);
						
					} catch (IOException e) {
					}
				    mProgressDialog.dismiss();					    
			}		
		}   
	 protected void SetMitPosition() {		
		 double lon1 =terstartlon;
		 double lon2 = terendlon;
		 double lat1 = terstartlat;
		 double lat2 =terendlat;		 
		 double dLon = Math.toRadians(lon2 - lon1);

	        lat1 = Math.toRadians(lat1);
	        lat2 = Math.toRadians(lat2);
	        lon1 = Math.toRadians(lon1);

	        double Bx = Math.cos(lat2) * Math.cos(dLon);
	        double By = Math.cos(lat2) * Math.sin(dLon);
	        finalLat= Math.toDegrees(Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By)));
	        finalLon= Math.toDegrees(lon1 + Math.atan2(By, Math.cos(lat1) + Bx));		   		  
	}
	 protected double DDMMmmmToDecimalLat(String coord,String hems)
		{
			String coorddegree=coord.substring(0,2);
			String coordminute=coord.substring(2,7);
			coordminute=coordminute.substring(0,2)+"."+coordminute.substring(2,5);
			double latcoordminute= Double.parseDouble(coordminute);
			latcoordminute=latcoordminute/60;
			DecimalFormat df = new DecimalFormat("0.000000");			
			coord=df.format(latcoordminute).substring(2);
			double result=Double.parseDouble(coorddegree + "." + coord);
			if(hems.equals("S"))
				result=-1*result;
			return result;	
		}
		protected double DDMMmmmToDecimalLon(String coord,String hems)
		{
			String coorddegree=coord.substring(0,3);
			String coordminute=coord.substring(3,8);
			coordminute=coordminute.substring(0,2)+"."+coordminute.substring(2,5);
			double latcoordminute= Double.parseDouble(coordminute);
			latcoordminute=latcoordminute/60;
			DecimalFormat df = new DecimalFormat("0.000000");			
			coord=df.format(latcoordminute).substring(2);
			double result=Double.parseDouble(coorddegree + "." + coord);
			if(hems.equals("W"))
				result=-1*result;
			return result;
		}
}
