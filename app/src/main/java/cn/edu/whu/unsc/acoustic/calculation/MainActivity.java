package cn.edu.whu.unsc.acoustic.calculation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;

import cn.edu.whu.unsc.acoustic.calculation.datasets.DataSets;
import cn.edu.whu.unsc.acoustic.calculation.datasets.Parameters;

public class MainActivity extends AppCompatActivity {
    static private String TAG = MainActivity.class.getName();

    private int mRows = 25;
    private int mColumns = 513;

    Parameters mParameters;
    DataSets mDataSets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializePermissions();

        final Button mSpectrumCalButton = findViewById(R.id.button_calculation);
        mSpectrumCalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                // Load Parameters
                Parameters mParameters = new Parameters();

                // Load Window Data
                DataSets mDataSets = new DataSets();
                mDataSets.setmP(loadDataSets());

                final double[][] tDS = mDataSets.getmP();

                mRows = 25;
                mColumns = 513;
                double[][] mTransposeDS = new double[25][513];
                for (int tRow = 0; tRow < mRows; tRow++) {
                    for (int tColumn = 0; tColumn < mColumns; tColumn++) {
                        mTransposeDS[tRow][tColumn] =tDS[tColumn][tRow];
                    }
                }

                ArrayList<double[]> tInputDS = new ArrayList<>();
                for (int tRow = 0; tRow < mRows; tRow++) {
                    tInputDS.add(mTransposeDS[tRow]);
                }

                double[] tVisualDetector = calculateSpectrogram(tInputDS, mRows, mColumns);
                calculateTimestamp(5, tVisualDetector[0], (int) tVisualDetector[1]);

            }
        });

        final Button mMatrixCalButton = findViewById(R.id.button_matrix_calculation);
        mMatrixCalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button


            }
        });

    }

    private double[][] loadDataSets() {

        mRows = 513;
        mColumns = 25;

        double[][] mDataSets = new double[mRows][mColumns];

        File mDataSetsFileDir = new File(this.getExternalFilesDir(null), "Calculation");

        if (!mDataSetsFileDir.exists()) {
            boolean directoryCreationStatus = mDataSetsFileDir.mkdirs();
            Log.i(TAG, "directoryCreationStatus: " + directoryCreationStatus);
        }

        File mSpectrogramFile = new File(mDataSetsFileDir, "2007P1W5.csv");

        if (mSpectrogramFile.exists()) {
            try {
                FileInputStream tFileInputStream = new FileInputStream(mSpectrogramFile);
                Scanner tScanner = new Scanner(tFileInputStream, "UTF-8");
                int tRowsCounter = 0;
                while (tScanner.hasNextLine()) {
                    String[] tLine = tScanner.nextLine().split(",");
                    for (int tColumnsCounter = 0; tColumnsCounter < mColumns; tColumnsCounter++) {
                        mDataSets[tRowsCounter][tColumnsCounter] = Double.valueOf(tLine[tColumnsCounter]);
                    }
                    tRowsCounter++;
                }
            }  catch (Exception e) {
                e.printStackTrace();
            }

        }

        return mDataSets;
    }

    private void initializePermissions() {
        initializeReadExternalStoragePermission();
    }

    private void initializeReadExternalStoragePermission() {
        int recordPermissionCheck = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (recordPermissionCheck != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
    }

    private double[] calculateSpectrogram(final ArrayList<double[]> SlideWindow, final int pRows, final int pColumns) {
        long tTimeCounterS = System.currentTimeMillis();

        // Chirp Features
        double mChirpLength = 0.045;
        double mFrequency16kHz = 16;
        double mFrequency21kHz = 21;

        // Detection Parameters
        int FFTLength = 1024;
        int HopLength = 128;
        int FFTWinSize = 4096;
        int timeResolutionRatio = (FFTWinSize - FFTLength) / HopLength + 1;
        int freqResolutionRatio = (FFTLength / 2) + 1;
        double mSampleRate = 44100.0;
        double mTimeInterval = HopLength / mSampleRate;
        double mFrequenceInterval = mSampleRate / FFTLength;

        int mInterceptIndex = 5;
        int mMinIndex = 1 + mInterceptIndex;
        int mMaxIndex = 40 - mInterceptIndex;
        int mFreq0 = 372;
        int mFreq1 = 488;

        double mDetLineDistance = 1.5;
        double mDetLineThreshold = 10.0;

        double[] m21kPoint = new double[2];
        double[] m16kPoint = new double[2];
        double[] mbIntercept = new double[2];
        double[] mBIntercept = new double[2];

        m21kPoint[0] = Math.round(mFrequency21kHz * 1000 / mFrequenceInterval) - mFreq0;
        m21kPoint[1] = 0.0;
        m16kPoint[0] = Math.round(mFrequency16kHz * 1000 / mFrequenceInterval) - mFreq0;
        m16kPoint[1] = Math.round((0.081269841269841 - 0.011609977324263) / mTimeInterval);

        double mTimeNum = mChirpLength / mTimeInterval;
        double mFrequenceNum = (mFrequency21kHz - mFrequency16kHz) * 1000 / mFrequenceInterval;
        double mTime2FreqRatio = mTimeNum / mFrequenceNum;

        mbIntercept[0] = m21kPoint[1] - mTime2FreqRatio * m21kPoint[0];
        mbIntercept[1] = m16kPoint[1] - mTime2FreqRatio * m16kPoint[0];
        mBIntercept[0] = m21kPoint[1] + mTime2FreqRatio * m16kPoint[0];
        mBIntercept[1] = m16kPoint[1] + mTime2FreqRatio * m21kPoint[0];

        double mMinSpectrogram = Double.MAX_VALUE;
        double mMaxSpectrogram = Double.MIN_VALUE;

        double mEpsilon = 2.2204e-16;
        double[][] tTransposeDS = new double[timeResolutionRatio][freqResolutionRatio];
        for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionRatio; tColumn++) {
                double tElement = SlideWindow.get(tRow)[tColumn];
                tTransposeDS[tRow][tColumn] = Math.abs(10.0 * Math.log10(Math.abs(tElement) + mEpsilon));
                if (tTransposeDS[tRow][tColumn] < mMinSpectrogram) {
                    mMinSpectrogram = tTransposeDS[tRow][tColumn];
                }
                if (tTransposeDS[tRow][tColumn] > mMaxSpectrogram) {
                    mMaxSpectrogram = tTransposeDS[tRow][tColumn];
                }
            }
        }

        double[][] tFlipudDS = new double[timeResolutionRatio][freqResolutionRatio];
        for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionRatio; tColumn++) {
                tFlipudDS[tRow][tColumn] = tTransposeDS[timeResolutionRatio - tRow - 1][tColumn];
            }
        }

        double mDetSpectrogram = 1.0 / (mMaxSpectrogram - mMinSpectrogram)  * 255;
        double[][] tNormalizedDS = new double[timeResolutionRatio][freqResolutionRatio];
        for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionRatio; tColumn++) {
                tNormalizedDS[tRow][tColumn] = (tFlipudDS[tRow][tColumn] - mMinSpectrogram) * mDetSpectrogram;
            }
        }

        //
        double[][] tFlipudDSSlice = new double[timeResolutionRatio][mFreq1 - mFreq0 + 1];
        for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
            for (int tColumn = 0; tColumn < mFreq1 - mFreq0 + 1; tColumn++) {
                tFlipudDSSlice[tRow][tColumn] = tNormalizedDS[tRow][mFreq0 + tColumn - 1];
            }
        }

        //
        int mMinRIntercept = -16;
        int mMaxRIntercept = 24;
        double[][] tSpectroRSta = new double[mMaxRIntercept - mMinRIntercept + 1][4];
        for (int tRow = 0; tRow < mMaxRIntercept - mMinRIntercept + 1; tRow++) {
            for (int tColumn = 0; tColumn < 4; tColumn++) {
                tSpectroRSta[tRow][tColumn] = 0.0;
            }
        }
        for (int tb = mMinRIntercept; tb <= mMaxRIntercept; tb++) {
            for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
                for (int tColumn = 0; tColumn < mFreq1 - mFreq0 + 1; tColumn++) {
                    double tDistance = Math.abs((timeResolutionRatio - tRow - 1) - (tColumn + 1 - 1) * mTime2FreqRatio - tb) / Math.sqrt(1.0 + mTime2FreqRatio * mTime2FreqRatio);
                    if (tDistance < mDetLineDistance) {
                        tSpectroRSta[tb - mMinRIntercept][0] += tFlipudDSSlice[tRow][tColumn];
                        tSpectroRSta[tb - mMinRIntercept][1] += 1;
                        tSpectroRSta[tb - mMinRIntercept][2] = tSpectroRSta[tb - mMinRIntercept][0] / tSpectroRSta[tb - mMinRIntercept][1];
                    }
//                    Log.i(TAG, "tDistance: " + tDistance);
                }
            }
        }
        for (int tb = 0; tb < mMaxRIntercept - mMinRIntercept; tb++) {
            tSpectroRSta[tb][3] = Math.abs(tSpectroRSta[tb + 1][2] - tSpectroRSta[tb][2]);
        }

        double[] tSpectroRDet = new double[mMaxIndex - mMinIndex + 1];
        for (int tb = 0; tb < mMaxIndex - mMinIndex + 1; tb++) {
            tSpectroRDet[tb] = tSpectroRSta[tb + mMinIndex - 1][3];
        }


        //
        int mMinLIntercept = 0;
        int mMaxLIntercept = 40;
        double[][] tSpectroLSta = new double[mMaxLIntercept - mMinLIntercept + 1][4];
        for (int tRow = 0; tRow < mMaxLIntercept - mMinLIntercept + 1; tRow++) {
            for (int tColumn = 0; tColumn < 4; tColumn++) {
                tSpectroLSta[tRow][tColumn] = 0.0;
            }
        }
        for (int tb = mMinLIntercept; tb <= mMaxLIntercept; tb++) {
            for (int tRow = 0; tRow < timeResolutionRatio; tRow++) {
                for (int tColumn = 0; tColumn < mFreq1 - mFreq0 + 1; tColumn++) {
                    double tDistance = Math.abs((timeResolutionRatio - tRow - 1) + (tColumn + 1 - 1) * mTime2FreqRatio - tb) / Math.sqrt(1.0 + mTime2FreqRatio * mTime2FreqRatio);
                    if (tDistance < mDetLineDistance) {
                        tSpectroLSta[tb - mMinLIntercept][0] += tFlipudDSSlice[tRow][tColumn];
                        tSpectroLSta[tb - mMinLIntercept][1] += 1;
                        tSpectroLSta[tb - mMinLIntercept][2] = tSpectroLSta[tb - mMinLIntercept][0] / tSpectroLSta[tb - mMinLIntercept][1];
                    }
//                    Log.i(TAG, "tDistance: " + tDistance);
                }
            }
        }
        for (int tb = 0; tb < mMaxLIntercept - mMinLIntercept; tb++) {
            tSpectroLSta[tb][3] = Math.abs(tSpectroLSta[tb + 1][2] - tSpectroLSta[tb][2]);
        }

        double[] tSpectroLDet = new double[mMaxIndex - mMinIndex + 1];
        for (int tb = 0; tb < mMaxIndex - mMinIndex + 1; tb++) {
            tSpectroLDet[tb] = tSpectroLSta[tb + mMinIndex - 1][3];
        }

        /*
         *
         */
        double tMaxSpectroRDet = 0.0;
        int tMaxSpectroRDetInd = 0;
        for (int tCounter = 0; tCounter < mMaxRIntercept - mMinRIntercept; tCounter++) {
            if (tSpectroRSta[tCounter][3] > tMaxSpectroRDet) {
                tMaxSpectroRDet = tSpectroRSta[tCounter][3];
                tMaxSpectroRDetInd = tCounter;
            }
        }

        double tMaxSpectroRDetPart = 0.0;
        int tMaxSpectroRDetIndPart = 0;
        for (int tCounter = 0; tCounter < mMaxIndex - mMinIndex + 1; tCounter++) {
            if (tSpectroRDet[tCounter] > tMaxSpectroRDetPart) {
                tMaxSpectroRDetPart = tSpectroRDet[tCounter];
                tMaxSpectroRDetIndPart = tCounter;
            }
        }


        double tMaxSpectroLDet = 0.0;
        int tMaxSpectroLDetInd = 0;
        for (int tCounter = 0; tCounter < mMaxRIntercept - mMinRIntercept; tCounter++) {
            if (tSpectroLSta[tCounter][3] > tMaxSpectroLDet) {
                tMaxSpectroLDet = tSpectroLSta[tCounter][3];
                tMaxSpectroLDetInd = tCounter;
            }
        }

        double tMaxSpectroLDetPart = 0.0;
        int tMaxSpectroLDetIndPart = 0;
        for (int tCounter = 0; tCounter < mMaxIndex - mMinIndex + 1; tCounter++) {
            if (tSpectroLDet[tCounter] > tMaxSpectroLDetPart) {
                tMaxSpectroLDetPart = tSpectroLDet[tCounter];
                tMaxSpectroLDetIndPart = tCounter;
            }
        }


        //
        int tClass = -2;
        double tInterceptEst = -1.0;
        if (tMaxSpectroRDetPart < mDetLineThreshold && tMaxSpectroLDetPart < mDetLineThreshold) {
            tInterceptEst = 0.0;
//            tClass = 0;
            tClass = -999;

        } else if (tMaxSpectroLDetPart > tMaxSpectroRDetPart) {
            tInterceptEst = tMaxSpectroLDetInd + 1 + mBIntercept[0] + mTime2FreqRatio * mFreq0;
//            tClass = -1;
            tClass = 1;

        } else if (tMaxSpectroLDetPart < tMaxSpectroRDetPart) {
            tInterceptEst = tMaxSpectroRDetInd + 1 + mbIntercept[0] - mTime2FreqRatio * mFreq0;
//            tClass = 1;
            tClass = 2;
        }

        long tTimeDuration = System.currentTimeMillis() - tTimeCounterS;

        Log.i(TAG, "Estimation: " + tInterceptEst + ":" + tClass);
        Log.i(TAG, "CalculationDuration: " + tTimeDuration);

        return new double[] { tInterceptEst, tClass};
    }

    private double[] calculateTimestamp(final int pWindowIndex, final double pIntercept, final double pType) {

        int FFTLength = 1024;

        double rTimeStamp = -1.0;
        double tType = -999.0;

        if (pType == 1.0) {
            tType = -1.0;
        } else if (pType == 2.0) {
            tType = 1.0;
        }

        double tSampleRate = 44100.0;
        double mWindowSize = 1024.0;
        double mStepSize = 128.0;

        double mTimeInterval = mStepSize / tSampleRate;
        double mFrequenceInterval = (tSampleRate / 2) / (mWindowSize / 2);

        double mChirpLength = 0.045;
        double mFrequency0KHz = 16;
        double mFrequency1KHz = 21;
        double mTimeNum = mChirpLength / mTimeInterval;
        double mFrequenceNum = (mFrequency1KHz - mFrequency0KHz) * 1000 / mFrequenceInterval;
        double mTime2FreqRatio = mTimeNum / mFrequenceNum;

        double m0TimeAxis = (mWindowSize / 2) / tSampleRate;
        double m16KFrequenceAxis = Math.ceil((mFrequency0KHz * 1000) / (tSampleRate / 2) * (mWindowSize / 2));

        double tTimeDeley4Window = (tType * mTime2FreqRatio * m16KFrequenceAxis + pIntercept) * mTimeInterval + m0TimeAxis;
        double tWindowDeley4TA = (((pWindowIndex - 1) * (FFTLength * 4 - 896 - 128 * 9)) + 1) / tSampleRate;

        if (pType == 1.0) {
            rTimeStamp = tTimeDeley4Window + tWindowDeley4TA - mChirpLength;
        } else if (pType == 2.0) {
            rTimeStamp = tTimeDeley4Window + tWindowDeley4TA;
        }

        return new double[] {rTimeStamp, -1.0};
    }

}
