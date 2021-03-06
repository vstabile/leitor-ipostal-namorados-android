/*==============================================================================
            Copyright (c) 2012-2013 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary

This  Vuforia(TM) sample application in source code form ("Sample Code") for the
Vuforia Software Development Kit and/or Vuforia Extension for Unity
(collectively, the "Vuforia SDK") may in all cases only be used in conjunction
with use of the Vuforia SDK, and is subject in all respects to all of the terms
and conditions of the Vuforia SDK License Agreement, which may be found at
https://developer.vuforia.com/legal/license.

By retaining or using the Sample Code in any manner, you confirm your agreement
to all the terms and conditions of the Vuforia SDK License Agreement.  If you do
not agree to all the terms and conditions of the Vuforia SDK License Agreement,
then you may not retain or use any of the Sample Code in any manner.


@file
    VideoPlayback.java

@brief
    This sample application shows how to play a video in AR mode.
    Devices that support video on texture can play the video directly
    on the image target.

    Other devices will play the video in full screen mode.
==============================================================================*/


package br.com.ipostal.reader;

import java.io.File;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import br.com.ipostal.reader.VideoPlayerHelper.MEDIA_STATE;
import br.com.ipostal.reader.VideoPlayerHelper.MEDIA_TYPE;

import com.qualcomm.QCAR.QCAR;

/** The AR activity for the VideoPlayback sample. */
public class VideoPlayback extends Activity
{    
    // SDCard location
    public static final String sdCardDir = Environment.getExternalStorageDirectory().toString() + "/iPostal";
    
    // Progress Dialog
    private ProgressDialog progressBar, progressDialog;
    
    // ProgressBar Handler
    private Handler progressBarHandler = new Handler();
 
    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0; 
    
	// Focus mode constants:
    private static final int FOCUS_MODE_NORMAL = 0;
    private static final int FOCUS_MODE_CONTINUOUS_AUTO = 1;
    
    // Application status constants:
    private static final int APPSTATUS_UNINITED         = -1;
    private static final int APPSTATUS_INIT_APP         = 0;
    private static final int APPSTATUS_INIT_QCAR        = 1;
    private static final int APPSTATUS_INIT_TRACKER     = 2;
    private static final int APPSTATUS_INIT_APP_AR      = 3;
    private static final int APPSTATUS_LOAD_TRACKER     = 4;
    private static final int APPSTATUS_INITED           = 5;
    private static final int APPSTATUS_CAMERA_STOPPED   = 6;
    private static final int APPSTATUS_CAMERA_RUNNING   = 7;

    // Name of the native dynamic libraries to load:
    private static final String NATIVE_LIB_SAMPLE       = "VideoPlayback";
    private static final String NATIVE_LIB_QCAR         = "QCAR";

    // Helpers to detect events such as double tapping:
    private GestureDetector mGestureDetector            = null;
    private SimpleOnGestureListener mSimpleListener     = null;

    // Pointer to the current activity:
    private Activity mCurrentActivity                   = null;

    // TODO
    // Movie for the Targets:
    public static final int NUM_TARGETS                 = 17;
    public static final int OVER_THE_RAINBOW            = 0;
    public static final int BETTER_TOGETHER             = 1;
    public static final int ALL_YOU_NEED                = 2;
    public static final int STOPMOTION                  = 3;
    public static final int LORO                  		= 4;
    public static final int HAPPY_FATHERS_DAY     		= 5;
    public static final int SUPER_HERO           		= 6;
    public static final int DIA_DE_PARABENS           	= 7;
    public static final int COMEMORAR           		= 8;
    public static final int BOLO_ROTATORIO           	= 9;
    public static final int BEBENDO_LEITE          		= 10;
    public static final int RENA_CANTANDO          		= 11;
    public static final int PASCOA          			= 12;
    public static final int DIA_DAS_MAES          		= 13;
    public static final int DIA_DOS_NAMORADOS			= 14;
    public static final int HEART						= 15;
    public static final int DINOSSAURO					= 16;
    
    private VideoPlayerHelper mVideoPlayerHelper[]      = null;
    private int mSeekPosition[]                         = null;
    private boolean mWasPlaying[]                       = null;
    private String mMovieName[]                         = null;

    // A boolean to indicate whether we come from full screen:
    private boolean mReturningFromFullScreen            = false;

    // Our OpenGL view:
    private QCARSampleGLView mGlView;

    // The StartupScreen view and the start button:
    private View mStartupView                           = null;
    private ImageView mStartButton                      = null;
    private ImageView mDownloadButton                   = null;
    private boolean mStartScreenShowing                 = false;

    // The view to display the sample splash screen:
    private ImageView mSplashScreenView;

    // The handler and runnable for the splash screen time out task:
    private Handler mSplashScreenHandler;
    private Runnable mSplashScreenRunnable;

    // The minimum time the splash screen should be visible:
    private static final long MIN_SPLASH_SCREEN_TIME    = 2000;

    // The time when the splash screen has become visible:
    long mSplashScreenStartTime = 0;

    // Our renderer:
    private VideoPlaybackRenderer mRenderer;

    // Display size of the device:
    private int mScreenWidth                            = 0;
    private int mScreenHeight                           = 0;

    // The current application status:
    private int mAppStatus                              = APPSTATUS_UNINITED;

    // The async tasks to initialize the QCAR SDK:
    private InitQCARTask mInitQCARTask;
    private LoadTrackerTask mLoadTrackerTask;

    // An object used for synchronizing QCAR initialization, dataset loading and
    // the Android onDestroy() life cycle event. If the application is destroyed
    // while a data set is still being loaded, then we wait for the loading
    // operation to finish before shutting down QCAR.
    private Object mShutdownLock = new Object();

    // QCAR initialization flags:
    private int mQCARFlags = 0;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    private int mSplashScreenImageResource = 0;
    
    // Current focus mode:
    private int mFocusMode;

    /** Static initializer block to load native libraries on start-up. */
    static
    {
        loadLibrary(NATIVE_LIB_QCAR);
        loadLibrary(NATIVE_LIB_SAMPLE);
    }

    /** An async task to initialize QCAR asynchronously. */
    private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;

        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mShutdownLock)
            {
                QCAR.setInitParameters(VideoPlayback.this, mQCARFlags);

                do
                {
                    // QCAR.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports 
                    // progress in percents (0 ... 100%).
                    // If QCAR.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = QCAR.init();

                    // Publish the progress value:
                    publishProgress(mProgressValue);

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true))
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that 
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0 
                         && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }


        protected void onPostExecute(Boolean result)
        {
            // Done initializing QCAR, proceed to next application
            // initialization status:
            if (result)
            {
                DebugLog.LOGD("InitQCARTask::onPostExecute: QCAR " +
                              "initialization successful");

                updateApplicationStatus(APPSTATUS_INIT_TRACKER);
            }
            else
            {
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder(VideoPlayback.this).create();
                dialogError.setButton
                (
                    DialogInterface.BUTTON_POSITIVE,
                    "Close",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Exiting application:
                            System.exit(1);
                        }
                    }
                );

                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED)
                {
                    logMessage = "Failed to initialize QCAR because this " +
                        "device is not supported.";
                }
                else
                {
                    logMessage = "Failed to initialize QCAR.";
                }

                // Log error:
                DebugLog.LOGE("InitQCARTask::onPostExecute: " + logMessage +
                                " Exiting.");

                // Show dialog box with error message:
                dialogError.setMessage(logMessage);
                dialogError.show();
            }
        }
    }


    /** An async task to load the tracker data asynchronously. */
    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap:
            synchronized (mShutdownLock)
            {
                // Load the tracker data set:
                return (loadTrackerData() > 0);
            }
        }

        protected void onPostExecute(Boolean result)
        {
            DebugLog.LOGD("LoadTrackerTask::onPostExecute: execution " +
                        (result ? "successful" : "failed"));

            if (result)
            {
                // Done loading the tracker, update application status:
                updateApplicationStatus(APPSTATUS_INITED);
            }
            else
            {
                // Create dialog box for display error:
                AlertDialog dialogError = new AlertDialog.Builder
                (
                    VideoPlayback.this
                ).create();

                dialogError.setButton
                (
                    DialogInterface.BUTTON_POSITIVE,
                    "Close",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Exiting application
                            System.exit(1);
                        }
                    }
                );

                // Show dialog box with error message:
                dialogError.setMessage("Failed to load tracker data.");
                dialogError.show();
            }
        }
    }

    private void storeScreenDimensions()
    {
        // Query display dimensions
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    /** Called when the activity first starts or the user navigates back
     * to an activity. */
    protected void onCreate(Bundle savedInstanceState)
    {
        DebugLog.LOGD("VideoPlayback::onCreate");
        super.onCreate(savedInstanceState);

        // Set the splash screen image to display during initialization:
        mSplashScreenImageResource = R.drawable.splash_screen;

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();

        // Query the QCAR initialization flags:
        mQCARFlags = getInitializationFlags();

        // Update the application status to start initializing application
        updateApplicationStatus(APPSTATUS_INIT_APP);

        // Create the gesture detector that will handle the single and 
        // double taps:
        mSimpleListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(
            getApplicationContext(), mSimpleListener);

        mVideoPlayerHelper = new VideoPlayerHelper[NUM_TARGETS];
        mSeekPosition = new int[NUM_TARGETS];
        mWasPlaying = new boolean[NUM_TARGETS];
        mMovieName = new String[NUM_TARGETS];

        // Create the video player helper that handles the playback of the movie
        // for the targets:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = new VideoPlayerHelper();
            mVideoPlayerHelper[i].init();
            mVideoPlayerHelper[i].setActivity(this);
        }
        // TODO
        mMovieName[OVER_THE_RAINBOW] = "over_the_rainbow.mp4";
        mMovieName[BETTER_TOGETHER] = "better_together.mp4";
        mMovieName[ALL_YOU_NEED] = "all_you_need.mp4";
        mMovieName[STOPMOTION] = "stopmotion.mp4";
        mMovieName[LORO] = "loro.mp4";
        mMovieName[HAPPY_FATHERS_DAY] = "happy_fathers_day.mp4";
        mMovieName[SUPER_HERO] = "super_hero.mp4";
        mMovieName[DIA_DE_PARABENS] = "dia_de_parabens.mp4";
        mMovieName[COMEMORAR] = "comemorar.mp4";
        mMovieName[BOLO_ROTATORIO] = "bolo_rotatorio.mp4";
        mMovieName[BEBENDO_LEITE] = "bebendo_leite.mp4";
        mMovieName[RENA_CANTANDO] = "rena_cantando.mp4";
        mMovieName[PASCOA] = "pascoa.mp4";
        mMovieName[DIA_DAS_MAES] = "dia_das_maes.mp4";
        mMovieName[DIA_DOS_NAMORADOS] = "dia_dos_namorados.mp4";
        mMovieName[HEART] = "heart.mp4";
        mMovieName[DINOSSAURO] = "dinossauro.mp4";

        mCurrentActivity = this;

        // Set the double tap listener:
        mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener()
        {
            /** Handle the double tap */
            public boolean onDoubleTap(MotionEvent e)
            {
                // Do not react if the StartupScreen is being displayed:
                if (mStartScreenShowing)
                    return false;

                for (int i = 0; i < NUM_TARGETS; i++)
                {
                	// TODO
                    String fileName = null;
                    switch (i) {
                    case 0:
						fileName = "over_the_rainbow.mp4";
						break;
                    case 1:
						fileName = "better_together.mp4";
						break;
                    case 2:
						fileName = "all_you_need.mp4";
						break;
                    case 3:
						fileName = "stopmotion.mp4";
						break;
                    case 4:
						fileName = "loro.mp4";
						break;
                    case 5:
						fileName = "happy_fathers_day.mp4";
						break;
                    case 6:
						fileName = "super_hero.mp4";
						break;
                    case 7:
						fileName = "dia_de_parabens.mp4";
						break;
                    case 8:
						fileName = "comemorar.mp4";
						break;
                    case 9:
						fileName = "bolo_rotatorio.mp4";
						break;
                    case 10:
						fileName = "bebendo_leite.mp4";
						break;
                    case 11:
						fileName = "rena_cantando.mp4";
						break;
                    case 12:
						fileName = "pascoa.mp4";
						break;
                    case 13:
						fileName = "dia_das_maes.mp4";
						break;
                    case 14:
						fileName = "dia_dos_namorados.mp4";
						break;
                    case 15:
                    	fileName = "heart.mp4";
                    	break;
                    case 16:
                    	fileName = "dinossauro.mp4";
                    	break;
					default:
						break;
					}
                	
                    // Verify that the tap happens inside the target:
                    if (isTapOnScreenInsideTarget(i, e.getX(), e.getY()))
                    {
                        // Check whether we can play full screen at all:
                        if (mVideoPlayerHelper[i].isPlayableFullscreen())
                        {
                        	Log.d("tap", "doubletap");
                        	
                            // Pause all other media:
                            pauseAll(i);

                            File file = new File(sdCardDir, fileName);
                            if (file.exists()) {
                            	// Request the playback in fullscreen:
                            	mVideoPlayerHelper[i].play(true,VideoPlayerHelper.CURRENT_POSITION);
                            } else {
                            	// Starta a DownloadActivity
                            	if (!isOnline(getApplicationContext())) {
                            		Toast.makeText(getApplicationContext(), getString(R.string.toast_no_connection), Toast.LENGTH_SHORT).show();
                            	} else {
	                            	Intent intent = new Intent(VideoPlayback.this, DownloadActivity.class);
	                            	intent.putExtra("fileName", fileName);
	                            	intent.putExtra("i", i);
	//                            	startActivity(intent);
	                            	startActivityForResult(intent, 2);
	                            	overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                            	}
                            }
                        }

                        // Even though multiple videos can be loaded only one 
                        // can be playing at any point in time. This break 
                        // prevents that, say, overlapping videos trigger 
                        // simultaneously playback.
                        break;
                    }
                }

                return true;
            }

            public boolean onDoubleTapEvent(MotionEvent e)
            {
                // We do not react to this event
                return false;
            }

            /** Handle the single tap */
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                // Do not react if the StartupScreen is being displayed
                if (mStartScreenShowing)
                    return false;

                for (int i = 0; i < NUM_TARGETS; i++)
                {
                    // Verify that the tap happened inside the target
                    if (isTapOnScreenInsideTarget(i, e.getX(), e.getY()))
                    {
                    	
                    	// TODO
                        String fileName = null;
                        switch (i) {
                        case 0:
							fileName = "over_the_rainbow.mp4";
							break;
                        case 1:
							fileName = "better_together.mp4";
							break;
                        case 2:
							fileName = "all_you_need.mp4";
							break;
                        case 3:
							fileName = "stopmotion.mp4";
							break;
                        case 4:
							fileName = "loro.mp4";
							break;
                        case 5:
    						fileName = "happy_fathers_day.mp4";
    						break;
                        case 6:
    						fileName = "super_hero.mp4";
    						break;
                        case 7:
    						fileName = "dia_de_parabens.mp4";
    						break;
                        case 8:
    						fileName = "comemorar.mp4";
    						break;
                        case 9:
    						fileName = "bolo_rotatorio.mp4";
    						break;
                        case 10:
    						fileName = "bebendo_leite.mp4";
    						break;
                        case 11:
    						fileName = "rena_cantando.mp4";
    						break;
                        case 12:
    						fileName = "pascoa.mp4";
    						break;
                        case 13:
    						fileName = "dia_das_maes.mp4";
    						break;
                        case 14:
    						fileName = "dia_dos_namorados.mp4";
    						break;
                        case 15:
                        	fileName = "heart.mp4";
                        	break;
                        case 16:
                        	fileName = "dinossauro.mp4";
                        	break;

						default:
							break;
						}
                    	
                        // Check if it is playable on texture
//                      if (mVideoPlayerHelper[i].isPlayableOnTexture())
                    	if ((mVideoPlayerHelper[i].isPlayableOnTexture() || mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.READY)
                    			&& mVideoPlayerHelper[i].getMediaType() != MEDIA_TYPE.FULLSCREEN)
                        	 
                        {
                            // We can play only if the movie was paused, ready or stopped
                            if ((mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PAUSED) ||
                                (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.READY)  ||
                                (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.STOPPED) ||
                                (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.REACHED_END))
                            {
                                // Pause all other media
                                pauseAll(i);

                                // If it has reached the end then rewind
                                if ((mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.REACHED_END))
                                    mSeekPosition[i] = 0;

                                // tries to load video from sdCard
                                try {
//                                	mVideoPlayerHelper[i].unload();
                                    
                                	Log.d("tap", "singletap texture");
                                	
                                    File file = new File(sdCardDir, fileName);
                                    if (file.exists()) {
//                                  	mVideoPlayerHelper[i].load( mMovieName[i], MEDIA_TYPE.ON_TEXTURE, true, 0);
                                    	mVideoPlayerHelper[i].play(false,VideoPlayerHelper.CURRENT_POSITION);
                                    } else {
                                    	if (!isOnline(getApplicationContext())) {
                                    		Toast.makeText(getApplicationContext(), getString(R.string.toast_no_connection), Toast.LENGTH_SHORT).show();
                                    	} else {
	                                    	// Starta a DownloadActivity
	                                    	Intent intent = new Intent(VideoPlayback.this, DownloadActivity.class);
	                                    	intent.putExtra("fileName", fileName);
	                                    	intent.putExtra("i", i);
//	                                    	startActivity(intent);
	                                    	startActivityForResult(intent, 2);
	                                    	overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                    	}
                                    }
                                } catch (Exception ex) {
                                  ex.printStackTrace();                               
                                }
                            }
                            else if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING)
                            {
                                // If it is playing then we pause it
                                mVideoPlayerHelper[i].pause();
                            }
                        }
                        else if (mVideoPlayerHelper[i].isPlayableFullscreen())
                        {
                        	Log.d("tap", "singletap fullscreen");
                        	
                        	 // tries to load video from sdCard
                            try {
//                            	mVideoPlayerHelper[i].unload();
                                
                                File file = new File(sdCardDir, fileName);
                                if (file.exists()) {
                                	// Request the playback in fullscreen:
                                    mVideoPlayerHelper[i].play(true,VideoPlayerHelper.CURRENT_POSITION);
//                             		mVideoPlayerHelper[i].load( mMovieName[i], MEDIA_TYPE.ON_TEXTURE_FULLSCREEN, true, 0);
                                } else {
                                	// Starta a DownloadActivity
                                	if (!isOnline(getApplicationContext())) {
                                		Toast.makeText(getApplicationContext(), getString(R.string.toast_no_connection), Toast.LENGTH_SHORT).show();
                                	} else {
	                                	Intent intent = new Intent(VideoPlayback.this, DownloadActivity.class);
	                                	intent.putExtra("fileName", fileName);
	                                	intent.putExtra("i", i);
//	                                	startActivity(intent);
	                                	startActivityForResult(intent, 2);
	                                	overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                	}
                                }
                            } catch (Exception ex) {
                              ex.printStackTrace();                               
                            }
                        	
                            // If it isn't playable on texture
                            // Either because it wasn't requested or because it
                            // isn't supported then request playback fullscreen.
//                            mVideoPlayerHelper[i].play(true,VideoPlayerHelper.CURRENT_POSITION);
                        }

                        // Even though multiple videos can be loaded only one 
                        // can be playing at any point in time. This break 
                        // prevents that, say, overlapping videos trigger 
                        // simultaneously playback.
                        break;
                    }
                }

                return true;
            }
        });
    }
    
//    private void getVideoFromAmazon(final String fileName, final int i){
//		
//		new AsyncTask<Void, Void, Boolean>() {
//			
//			protected void onPreExecute() {		
//				progressBar = new ProgressDialog(VideoPlayback.this);
//	            progressBar.setMessage(getString(R.string.downloading_video));
//	            progressBar.setIndeterminate(false);
//	            progressBar.setMax(100);
//	            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//	            progressBar.setCancelable(true);
//	            progressBar.show();
//	            	            
//	            WindowManager.LayoutParams lp = progressBar.getWindow().getAttributes();  
//	            lp.dimAmount = 1.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
//	            progressBar.getWindow().setAttributes(lp);
//	            	           
//	            Handler handler = new Handler(); 
//	            handler.postDelayed(new Runnable() { 
//	                 public void run() { 
//	                	 mRenderer.mIsDownloading = true;
//	                 } 
//	            }, 2000); 
//			}
//			
//			@Override
//			protected Boolean doInBackground(Void... params) {
//				Boolean success = false;
//				try {
//					  
//              	  AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(
//            				Constants.AWS_ACCESS_KEY_ID, Constants.AWS_SECRET_ACCESS_KEY));
//              	  s3Client.setEndpoint(Constants.AWS_ENDPOINT);
//              	  
//              	  Log.d("fileName", fileName);
//              	  File dir = new File(sdCardDir); 
//              	  dir.mkdirs();
//              	  File file = new File(sdCardDir, fileName); 
//            	  file.createNewFile();
//            	  
//            	// Zera a progressBar
//   				  progressBarHandler.post(new Runnable() {
//   					public void run() {
//   						progressBar.setProgress(0);
//   					}
//				  });
//            	  
//            	  ProgressListener listener = new ProgressListener() {
//              		  float total = 0;
//              		  float progress = 0;
//              		  
//				      @Override
//				      public void progressChanged(ProgressEvent pv) {
//				    	  total += (int) pv.getBytesTransfered();
//				    	  // o 140000 � o total de bytes do arquivo hardcoded
//		                     progress = (total / 140000) * 100;
//		                     
//		                     // Update the progress bar
//			   				  progressBarHandler.post(new Runnable() {
//			   					public void run() {
//			   						Log.d("PROGRESS", String.valueOf(progress));
//			   						progressBar.setProgress((int) progress);
//			   					}
//							  });
//				      }
//				
//				  };
//            	  
//            	  GetObjectRequest objReq = new GetObjectRequest(Constants.AWS_PICTURE_BUCKET,
//                			"production/" + fileName);
//            	  
//            	  objReq.setProgressListener(listener);
//              	  
//            	  ObjectMetadata object = s3Client.getObject(objReq, file);
//            	  
////            	  S3Object object = s3Client.getObject(new GetObjectRequest(Constants.AWS_PICTURE_BUCKET,
////                			"production/" + fileName));
//              	  
//              	  success = true;
//              	  
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				
//				return success;
//			}
//			
//			protected void onPostExecute(Boolean success) {
////				progressBar.dismiss();		
//				
//				mRenderer.mIsDownloading = false;
//				
//				if (progressBar != null)
//				{
//					progressBar.dismiss();
//					progressBar = null;
//				}
//				
//				if (success) {
//					Log.d("DOWNLOAD AWS", "success");
//					mVideoPlayerHelper[i].load( mMovieName[i], MEDIA_TYPE.ON_TEXTURE, true, 0);
//				} else {
//					Log.d("DOWNLOAD AWS", "fail");
//					mVideoPlayerHelper[i].load( mMovieName[i], MEDIA_TYPE.ON_TEXTURE, true, 0);
//				}
//			}
//			
//		}.execute();
//		
//	}
    
        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
//            pDialog.setProgress(Integer.parseInt(progress[0]));
       }
         
    /**
     * Showing Dialog
     * */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case progress_bar_type:
            progressBar = new ProgressDialog(this);
            progressBar.setMessage("Downloading file. Please wait...");
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressBar.setCancelable(false);
            progressBar.show();
            return progressBar;
        case 1:
        	progressDialog = new ProgressDialog(this);
        	progressDialog.setMessage("Downloading...");
        	progressDialog.setCancelable(false);
        	progressDialog.show();
            return progressDialog;
        default:
            return null;
        }
    }


    /** We want to load specific textures from the APK, which we will later
     * use for rendering. */
    private void loadTextures()
    {
    	// TODO
    	mTextures.add(Texture.loadTextureFromApk("over_the_rainbow.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("better_together.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("all_you_need.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("stopmotion.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("loro.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("happy_fathers_day.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("super_hero.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("dia_de_parabens.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("comemorar.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("bolo_rotatorio.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("bebendo_leite.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("rena_cantando.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("pascoa.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("dia_das_maes.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("dia_dos_namorados.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("heart.png",
                getAssets()));
    	mTextures.add(Texture.loadTextureFromApk("dinossauro.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("play.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("busy.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("error.png",
                getAssets()));
    }


    /** Configure QCAR with the desired version of OpenGL ES. */
    private int getInitializationFlags()
    {
        return QCAR.GL_20;
    }


    /** Native tracker initialization and deinitialization. */
    public native int initTracker();
    public native void deinitTracker();

    /** Native functions to load and destroy tracking data. */
    public native int loadTrackerData();
    public native void destroyTrackerData();

    /** Native sample initialization. */
    public native void onQCARInitializedNative();

    /** Native methods for starting and stopping the camera. */
    private native void startCamera();
    private native void stopCamera();

    /** Native method for setting / updating the projection matrix for 
     * AR content rendering */
    private native void setProjectionMatrix();

    private native boolean isTapOnScreenInsideTarget(
        int target, float x, float y);

   /** Called when the activity will start interacting with the user.*/
    protected void onResume()
    {
        DebugLog.LOGD("VideoPlayback::onResume");
        super.onResume();

        // QCAR-specific resume operation
        QCAR.onResume();

        // We may start the camera only if the QCAR SDK has already been
        // initialized:
        if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
        }

        // Setup the start button:
        setupStartButton();

        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

        // Do not show the startup screen if we're returning from full screen:
        if (!mReturningFromFullScreen)
            showStartupScreen();

        // Reload all the movies
        if (mRenderer != null)
        {
            for (int i = 0; i < NUM_TARGETS; i++)
            {
                if (!mReturningFromFullScreen)
                {
                    mRenderer.requestLoad(
                        i, mMovieName[i], mSeekPosition[i], false);
                }
                else
                {
                    mRenderer.requestLoad(
                        i, mMovieName[i], mSeekPosition[i], mWasPlaying[i]);
                }
            }
        }

        mReturningFromFullScreen = false;
    }

    /** Called when returning from the full screen player */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1)
        {
            if (resultCode == RESULT_OK)
            {
                // The following values are used to indicate the position in 
                // which the video was being played and whether it was being 
                // played or not:
                String movieBeingPlayed = data.getStringExtra("movieName");
                mReturningFromFullScreen = true;

                // Find the movie that was being played full screen
                for (int i = 0; i < NUM_TARGETS; i++)
                {
                    if (movieBeingPlayed.compareTo(mMovieName[i]) == 0)
                    {
                        mSeekPosition[i] = data.getIntExtra("currentSeekPosition", 0);
                        mWasPlaying[i] = data.getBooleanExtra("playing", false);
                    }
                }
            }
        }
        if (requestCode == 2){
        	if (resultCode == RESULT_OK) {
        		hideStartupScreen();
        		mReturningFromFullScreen = true;
        	}
        }
    }

    public void onConfigurationChanged(Configuration config)
    {
        DebugLog.LOGD("VideoPlayback::onConfigurationChanged");
        super.onConfigurationChanged(config);

        storeScreenDimensions();

        // Set projection matrix:
        if (QCAR.isInitialized() && (mAppStatus == APPSTATUS_CAMERA_RUNNING))
            setProjectionMatrix();
    }


    /** Called when the system is about to start resuming a previous activity.*/
    protected void onPause()
    {
        DebugLog.LOGD("VideoPlayback::onPause");
        super.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
        }

        // Store the playback state of the movies and unload them:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // If the activity is paused we need to store the position in which 
            // this was currently playing:
            if (mVideoPlayerHelper[i].isPlayableOnTexture())
            {
                mSeekPosition[i] = mVideoPlayerHelper[i].getCurrentPosition();
                mWasPlaying[i] = mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING ? true : false;
            }

            // We also need to release the resources used by the helper, though
            // we don't need to destroy it:
            if (mVideoPlayerHelper[i]!= null)
                mVideoPlayerHelper[i].unload();
        }

        // Hide the Startup View:
        hideStartupScreen();

        mReturningFromFullScreen = false;

        // QCAR-specific pause operation:
        QCAR.onPause();
    }


    /** Native function to deinitialize the application.*/
    private native void deinitApplicationNative();


    /** The final call you receive before your activity is destroyed.*/
    protected void onDestroy()
    {
        DebugLog.LOGD("VideoPlayback::onDestroy");
        super.onDestroy();

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // If the activity is destroyed we need to release all resources:
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].deinit();
            mVideoPlayerHelper[i] = null;
        }

        // Dismiss the splash screen time out handler:
        if (mSplashScreenHandler != null)
        {
            mSplashScreenHandler.removeCallbacks(mSplashScreenRunnable);
            mSplashScreenRunnable = null;
            mSplashScreenHandler = null;
        }

        // Cancel potentially running tasks:
        if (mInitQCARTask != null &&
            mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            mInitQCARTask.cancel(true);
            mInitQCARTask = null;
        }

        if (mLoadTrackerTask != null &&
            mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        // Ensure that all asynchronous operations to initialize QCAR and 
        // loading the tracker datasets do not overlap:
        synchronized (mShutdownLock) {

            // Do application deinitialization in native code:
            deinitApplicationNative();

            // Unload texture:
            mTextures.clear();
            mTextures = null;

            // Destroy the tracking data set:
            destroyTrackerData();

            // Deinit the tracker:
            deinitTracker();

            // Deinitialize QCAR SDK:
            QCAR.deinit();
        }

        System.gc();
    }


    /** NOTE: this method is synchronized because of a potential concurrent
     * access by VideoPlayback::onResume() and InitQCARTask::onPostExecute(). */
    private synchronized void updateApplicationStatus(int appStatus)
    {
        // Exit if there is no change in status:
        if (mAppStatus == appStatus)
            return;

        // Store new status value:
        mAppStatus = appStatus;

        // Execute application state-specific actions:
        switch (mAppStatus)
        {
            case APPSTATUS_INIT_APP:
                // Initialize application elements that do not rely on QCAR
                // initialization:
                initApplication();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_INIT_QCAR);
                break;

            case APPSTATUS_INIT_QCAR:
                // Initialize QCAR SDK asynchronously to avoid blocking the
                // main (UI) thread.
                //
                // NOTE: This task instance must be created and invoked on the
                // UI thread and it can be executed only once!
                try
                {
                    mInitQCARTask = new InitQCARTask();
                    mInitQCARTask.execute();
                }
                catch (Exception e)
                {
                    DebugLog.LOGE("Initializing QCAR SDK failed");
                }
                break;

            case APPSTATUS_INIT_TRACKER:
                // Initialize the ImageTracker:
                if (initTracker() > 0)
                {
                    // Proceed to next application initialization status:
                    updateApplicationStatus(APPSTATUS_INIT_APP_AR);
                }
                break;

            case APPSTATUS_INIT_APP_AR:
                // Initialize Augmented Reality-specific application elements
                // that may rely on the fact that the QCAR SDK has been
                // already initialized:
                initApplicationAR();

                // Proceed to next application initialization status:
                updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
                break;

            case APPSTATUS_LOAD_TRACKER:
                // Load the tracking data set:
                //
                // NOTE: This task instance must be created and invoked on the 
                // UI thread and it can be executed only once!
                try
                {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    DebugLog.LOGE("Loading tracking data set failed");
                }
                break;

            case APPSTATUS_INITED:
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector.
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                // Native post initialization:
                onQCARInitializedNative();

                // The elapsed time since the splash screen was visible:
                long splashScreenTime = System.currentTimeMillis() -
                                            mSplashScreenStartTime;
                long newSplashScreenTime = 0;
                if (splashScreenTime < MIN_SPLASH_SCREEN_TIME)
                {
                    newSplashScreenTime = MIN_SPLASH_SCREEN_TIME -
                                            splashScreenTime;
                }

                // Request a callback function after a given timeout to dismiss
                // the splash screen:
                mSplashScreenHandler = new Handler();
                mSplashScreenRunnable =
                    new Runnable() {
                        public void run()
                        {
                            // Hide the splash screen:
                            mSplashScreenView.setVisibility(View.INVISIBLE);

                            // Activate the renderer:
                            mRenderer.mIsActive = true;

                            // Now add the GL surface view. It is important
                            // that the OpenGL ES surface view gets added
                            // BEFORE the camera is started and video
                            // background is configured.
                            addContentView(mGlView, new LayoutParams(
                                            LayoutParams.MATCH_PARENT,
                                            LayoutParams.MATCH_PARENT));

                            // Setup the start screen:
                            setupStartScreen();

                            // Setup the start button:
                            setupStartButton();

                            // Start the camera:
                            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
                        }
                };

                mSplashScreenHandler.postDelayed(mSplashScreenRunnable,
                                                    newSplashScreenTime);
                break;

            case APPSTATUS_CAMERA_STOPPED:
                // Call the native function to stop the camera:
                stopCamera();
                break;

            case APPSTATUS_CAMERA_RUNNING:
                // Call the native function to start the camera:
                startCamera();
                setProjectionMatrix();
                
                // Set continuous auto-focus if supported by the device,
                // otherwise default back to regular auto-focus mode.
                mFocusMode = FOCUS_MODE_CONTINUOUS_AUTO;
                if(!setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO))
                {
                    mFocusMode = FOCUS_MODE_NORMAL;
                    setFocusMode(FOCUS_MODE_NORMAL);
                }

                break;

            default:
                throw new RuntimeException("Invalid application state");
        }
    }

    /** This call sets the start screen up, adds it to the view and pads the 
     * text to something nice */
    private void setupStartScreen()
    {
        // Inflate the view from the xml file:
        mStartupView = getLayoutInflater().inflate(
            R.layout.startup_screen, null);

        // Add it to the content view:
        addContentView(mStartupView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));


        mStartScreenShowing = true;
    }

    /** This call sets the start button variable up */
    private void setupStartButton()
    {
        mStartButton = (ImageView) findViewById(R.id.startButton);
        mDownloadButton = (ImageView) findViewById(R.id.downloadButton);

        if (mStartButton != null)
        {
            // Setup a click listener that hides the StartupScreen:
            mStartButton.setOnClickListener(new ImageView.OnClickListener() {
                    public void onClick(View arg0) {
                        hideStartupScreen();
                    }
            });
        }
        
        if (mDownloadButton != null)
        {
            // Setup a click listener that downloads iPostal
        	mDownloadButton.setOnClickListener(new ImageView.OnClickListener() {
                    public void onClick(View arg0) {
                    	final String appName = "br.com.ipostal";
                    	if (isMarket(getApplicationContext())){
                    		try {
                        	    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
                        	} catch (android.content.ActivityNotFoundException anfe) {
                        	    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
                        	}
                        } else {
                            Intent goToAmazonStore = new Intent(Intent.ACTION_VIEW,
                            		Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + appName));
                            goToAmazonStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(goToAmazonStore);
                        }
                    }
            });
        }
    }

    /** Show the startup screen */
    private void showStartupScreen()
    {
        if (mStartupView != null)
        {
            mStartupView.setVisibility(View.VISIBLE);
            mStartScreenShowing = true;
        }
    }

    /** Hide the startup screen */
    private void hideStartupScreen()
    {
        if (mStartupView != null)
        {
            mStartupView.setVisibility(View.INVISIBLE);
            mStartScreenShowing = false;
        }
    }

    /** Pause all movies except one
        if the value of 'except' is -1 then
        do a blanket pause */
    private void pauseAll(int except)
    {
        // And pause all the playing videos:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // We can make one exception to the pause all calls:
            if (i != except)
            {
                // Check if the video is playable on texture
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // If it is playing then we pause it
                    mVideoPlayerHelper[i].pause();
                }
            }
        }
    }

    /** Do not exit immediately and instead show the startup screen */
    public void onBackPressed() {

        // If this is the first time the back button is pressed
        // show the StartupScreen and pause all media:
        if (!mStartScreenShowing)
        {
            // Show the startup screen:
            showStartupScreen();

            pauseAll(-1);
        }
        else // if this is the second time the user pressed the back button
        {
            // Hide the Startup View:
            hideStartupScreen();

            // And exit:
            super.onBackPressed();
        }
    }


    /** Tells native code whether we are in portrait or landscape mode */
    private native void setActivityPortraitMode(boolean isPortrait);


    /** Initialize application GUI elements that are not related to AR. */
    private void initApplication()
    {
        // Set the screen orientation:
        int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        // Apply screen orientation:
        setRequestedOrientation(screenOrientation);

        // Pass on screen orientation info to native code:
        setActivityPortraitMode(
            screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        storeScreenDimensions();

        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright:
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create and add the splash screen view:
        mSplashScreenView = new ImageView(this);
        mSplashScreenView.setImageResource(mSplashScreenImageResource);
        addContentView(mSplashScreenView, new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mSplashScreenStartTime = System.currentTimeMillis();

    }


    /** Native function to initialize the application. */
    private native void initApplicationNative(int width, int height);


    /** Initializes AR application components. */
    private void initApplicationAR()
    {
        // Do application initialization in native code (e.g. registering
        // callbacks, etc.):
        initApplicationNative(mScreenWidth, mScreenHeight);

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = QCAR.requiresAlpha();

        mGlView = new QCARSampleGLView(this);
        mGlView.init(mQCARFlags, translucent, depthSize, stencilSize);

        mRenderer = new VideoPlaybackRenderer();

        // The renderer comes has the OpenGL context, thus, loading to texture
        // must happen when the surface has been created. This means that we
        // can't load the movie from this thread (GUI) but instead we must
        // tell the GL thread to load it once the surface has been created.
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mRenderer.setVideoPlayerHelper(i, mVideoPlayerHelper[i]);
            mRenderer.requestLoad(i, mMovieName[i], 0, false);
        }

        mGlView.setRenderer(mRenderer);
    }
    
    /** Invoked every time before the options menu gets displayed to give
     *  the Activity a chance to populate its Menu with menu items. */
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        super.onPrepareOptionsMenu(menu);
        
        menu.clear();

        if(mFocusMode == FOCUS_MODE_CONTINUOUS_AUTO)
            menu.add(getString(R.string.deact_continuous_focus));
        else
            menu.add(getString(R.string.act_continuous_focus));

        menu.add(getString(R.string.trigger_auto_focus));

        return true;
    }

    /** Invoked when the user selects an item from the Menu */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getTitle().equals(getString(R.string.act_continuous_focus)))
        {
            if(setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO))
            {
                mFocusMode = FOCUS_MODE_CONTINUOUS_AUTO;
                item.setTitle(getString(R.string.deact_continuous_focus));
            }
            else
            {
                Toast.makeText
                (
                    this,
                    "Unable to activate Continuous Auto-Focus",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
        else if(item.getTitle().equals(getString(R.string.deact_continuous_focus)))
        {
            if(setFocusMode(FOCUS_MODE_NORMAL))
            {
                mFocusMode = FOCUS_MODE_NORMAL;
                item.setTitle(getString(R.string.act_continuous_focus));
            }
            else
            {
                Toast.makeText
                (
                    this,
                    "Unable to deactivate Continuous Auto-Focus",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
        else if(item.getTitle().equals(getString(R.string.trigger_auto_focus)))
        {
            boolean result = autofocus();
            
            // Autofocus action resets focus mode
            mFocusMode = FOCUS_MODE_NORMAL;
            
            DebugLog.LOGI
            (
                "Autofocus requested" +
                (result ?
                    " successfully." :
                    ". Not supported in current mode or on this device.")
            );
        }

        return true;
    }
    
    private native boolean autofocus();
    private native boolean setFocusMode(int mode);

    /** Returns the number of registered textures. */
    public int getTextureCount()
    {
        return mTextures.size();
    }


    /** Returns the texture object at the specified index. */
    public Texture getTexture(int i)
    {
        return mTextures.elementAt(i);
    }


    /** A helper for loading native libraries stored in "libs/armeabi*". */
    public static boolean loadLibrary(String nLibName)
    {
        try
        {
            System.loadLibrary(nLibName);
            DebugLog.LOGI("Native library lib" + nLibName + ".so loaded");
            return true;
        }
        catch (UnsatisfiedLinkError ulee)
        {
            DebugLog.LOGE("The library lib" + nLibName +
                            ".so could not be loaded");
        }
        catch (SecurityException se)
        {
            DebugLog.LOGE("The library lib" + nLibName +
                            ".so was not allowed to be loaded");
        }

        return false;
    }

    /** We do not handle the touch event here, we just forward it to the 
     * gesture detector */
    public boolean onTouchEvent(MotionEvent event)
    {
        return mGestureDetector.onTouchEvent(event);
    }
    
    public static boolean isOnline(Context context) {
	    ConnectivityManager cm =
	        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}
    
    public static boolean isMarket(Context context){
        int currentSig = 1; // I just set this to 1 to avoid any exceptions later on.
        try {
            Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            for (Signature sig : sigs)
            {
                currentSig = sig.hashCode();
                Log.i("MyApp", "Signature hashcode : " + sig.hashCode());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        
        if (currentSig == 401964109){
            return true;
        } else {
            return false;
        }
    }
    
}
