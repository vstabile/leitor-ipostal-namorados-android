package br.com.ipostal.reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class DownloadActivity extends Activity {
	
    private ProgressDialog progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		// TODO
//		getWindow().setWindowAnimations();
		
		setContentView(R.layout.clear);
		
		Intent intent = getIntent();
		String fileName = intent.getStringExtra("fileName");
		int i = intent.getIntExtra("i", -1);
		
		DownloadFileFromURL(fileName, i);
		
	}
	
	private void DownloadFileFromURL(final String fileName, final int i){
	    
	    /**
	     * Background Async Task to download file
	     * */
	    new AsyncTask<String, String, String>() {
	     
	        @Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            
	            progressBar = new ProgressDialog(DownloadActivity.this);
	            progressBar.setMessage(getString(R.string.downloading_video));
	            progressBar.setIndeterminate(false);
	            progressBar.setMax(100);
	            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	            progressBar.setCancelable(false);
	            progressBar.setCanceledOnTouchOutside(false);
	            progressBar.show();
	        }
	     
	        @Override
	        protected String doInBackground(String... f_url) {
	            int count;
	            try {
	            	
	            	Log.d("fileName", fileName);
              	  	File dir = new File(VideoPlayback.sdCardDir); 
              	  	dir.mkdirs();
              	  	File file = new File(VideoPlayback.sdCardDir, fileName); 
              	  	file.createNewFile();
	            	
              	  	Log.d("Download", "opening connection");
	            	String strUrl = "https://s3-sa-east-1.amazonaws.com/ipostal.videos/production/" + fileName;
	                URL url = new URL(strUrl);
	                URLConnection conection = url.openConnection();
	                conection.connect();
	                Log.d("Download", "connection opened");
	                
	                // getting file length
	                Log.d("Download", "getting file length");
	                int lenghtOfFile = conection.getContentLength();
	                Log.d("Download", "got file length");
	     
	                // input stream to read file - with 8k buffer
	                Log.d("Download", "creating inputstream");
	                InputStream input = new BufferedInputStream(url.openStream(), 8192);
	                Log.d("Download", "inputstream created");
	     
	                // Output stream to write file
	                Log.d("Download", "creating outputstream");
	                OutputStream output = new FileOutputStream(VideoPlayback.sdCardDir + "/" + fileName);
	                Log.d("Download", "outputstream created");
	     
	                byte data[] = new byte[1024];
	     
	                long total = 0;
	     
	                Log.d("Download", "starting download");
	                while ((count = input.read(data)) != -1) {
	                    total += count;
	                    // publishing the progress....
	                    // After this onProgressUpdate will be called
	                    publishProgress(""+(int)((total*100)/lenghtOfFile));
	                    	     
	                    // writing data to file
	                    output.write(data, 0, count);
	                }
	     
	                // flushing output
	                output.flush();
	                Log.d("Download", "Output flushed");
	     
	                // closing streams
	                output.close();
	                input.close();
	                Log.d("Download", "Streams closed");
	     
	            } catch (Exception e) {
	                Log.e("Error: ", e.getMessage());
	            }
	     
	            return null;
	        }
	     
	        protected void onProgressUpdate(String... progress) {
	            // setting progress percentage
	        	progressBar.setProgress(Integer.parseInt(progress[0]));
	       }
	     
	        @Override
	        protected void onPostExecute(String file_url) {
				
				if (progressBar != null)
				{
					progressBar.dismiss();
					progressBar = null;
				}
				
				Log.d("DOWNLOAD AWS", "success");
				
//				Intent intent = new Intent(DownloadActivity.this, VideoPlayback.class);
//				startActivity(intent);
				setResult(RESULT_OK);
				finish();
	        }
	     
	    }.execute();
	}

}
