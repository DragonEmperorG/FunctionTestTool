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
//                        mTransposeDS[tRow][tColumn] =tDS[tColumn][tRow];
                        mTransposeDS[tRow][tColumn] = tDS[tRow][tColumn];
                    }
                }

                ArrayList<double[]> tInputDS = new ArrayList<>();
                for (int tRow = 0; tRow < mRows; tRow++) {
                    tInputDS.add(mTransposeDS[tRow]);
                }

                double[] tVisualDetector1 = detectChirp(tInputDS, mRows, mColumns, 1);
                calculateTimestamp(4, tVisualDetector1[1], (int) tVisualDetector1[0]);
                double[] tVisualDetector2 = detectChirp(tInputDS, mRows, mColumns, 2);
                calculateTimestamp(4, tVisualDetector2[1], (int) tVisualDetector2[0]);

            }
        });

        final Button mMatrixCalButton = findViewById(R.id.button_matrix_calculation);
        mMatrixCalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                loop1:
                for (int x = 0; x < 4; x++) {
                    boolean flag = true;

                    loop2:
                    for (int y = 0; y < 5; y++) {
                        if (x == 2) {
                            flag = false;
                            break;
                        }
                        Log.i(TAG, "Loop: " + "x=" + x + ",y=" + y);
                    }

                    if (flag) {
                        Log.i(TAG, "Loop: " + "x=" + x);
                    }
                }

            }
        });

    }

    private double[][] loadDataSets() {

        mRows = 25;
        mColumns = 513;

        double[][] mDataSets = new double[mRows][mColumns];

        File mDataSetsFileDir = new File(this.getExternalFilesDir(null), "Calculation");

        if (!mDataSetsFileDir.exists()) {
            boolean directoryCreationStatus = mDataSetsFileDir.mkdirs();
            Log.i(TAG, "directoryCreationStatus: " + directoryCreationStatus);
        }

        File mSpectrogramFile = new File(mDataSetsFileDir, "ZL40mMatrix4.csv");

        if (mSpectrogramFile.exists()) {
            try {
                FileInputStream tFileInputStream = new FileInputStream(mSpectrogramFile);
                Scanner tScanner = new Scanner(tFileInputStream, "UTF-8");
                int tRowsCounter = 0;
                while (tScanner.hasNextLine()) {
                    String[] tLine = tScanner.nextLine().split(" ,");
                    for (int tColumnsCounter = 0; tColumnsCounter < mColumns; tColumnsCounter++) {
                        String tString = tLine[tColumnsCounter];
                        mDataSets[tRowsCounter][tColumnsCounter] = Double.valueOf(tString);
                    }
                    tRowsCounter++;
                }
            } catch (Exception e) {
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

    private double[] detectChirp(final ArrayList<double[]> timeFrequenceMatrix, final int rows, final int columns, final int frequenceBandFlag) {
        long timeCounter = System.currentTimeMillis();

        // Chirp Features
        final double chirpLength = 0.045;    // TODO: 0.045
        final double frequency15kHz = 15000.0;
        final double frequency18kHz = 18000.0;
        final double frequency19kHz = 19000.0;
        final double frequency22kHz = 22000.0;

        // Detection Parameters
        final int skipThreshold = 5;
        final double bufferThreshold = 1.5;
        final double gradientThreshold = 10.0;

        // STFT Parameters
        final int fftWindowLength = 4096;
        final int fftLength = 1024;
        final int hopLength = 128;
        final int timeResolutionDiscrete = (fftWindowLength - fftLength) / hopLength + 1;
        final int freqResolutionDiscrete = (fftLength / 2) + 1;
        final double timeResolutionContinuous = timeResolutionDiscrete - 1.0;
        final double freqResolutionContinuous = freqResolutionDiscrete - 1.0;
        final double sampleRate = 44100.0;
        final double timeResolutionInterval = hopLength / sampleRate;
        final double freqResolutionInterval = sampleRate / fftLength;
        final double freqResolutionIntervalInverse = fftLength / sampleRate;

        final int freq15kDiscrete = (int) Math.round(frequency15kHz * freqResolutionIntervalInverse) + 1;    // 348 + 1
        final int freq18kDiscrete = (int) Math.round(frequency18kHz * freqResolutionIntervalInverse) + 1;    // 418 + 1
        final int freq19kDiscrete = (int) Math.round(frequency19kHz * freqResolutionIntervalInverse) + 1;    // 441 + 1
        final int freq22kDiscrete = (int) Math.round(frequency22kHz * freqResolutionIntervalInverse) + 1;    // 511 + 1

        int freqLowerBoundDiscrete = -1;
        int freqUpperBoundDiscrete = -1;
        double chirpTimeFraction = chirpLength / timeResolutionInterval;
        double chirpFreqFraction = -1.0;
        double chirpTime2FreqRatio;
        int chirpTimeDiscrete = (int) Math.round(chirpTimeFraction);
        int detectorBoxTimeLowerBoundDiscrete = 0;
        int detectorBoxTimeUpperBoundDiscrete = (int) timeResolutionContinuous;
        int detectorBoxFreqLowerBoundDiscrete = 0;
        int detectorBoxFreqUpperBoundDiscrete = 0;

        if (frequenceBandFlag == 1) {
            chirpFreqFraction = (frequency18kHz - frequency15kHz) * freqResolutionIntervalInverse;
            freqLowerBoundDiscrete = freq15kDiscrete;
            freqUpperBoundDiscrete = freq18kDiscrete;
            detectorBoxFreqUpperBoundDiscrete = freq18kDiscrete - freq15kDiscrete;
        } else if (frequenceBandFlag == 2) {
            chirpFreqFraction = (frequency22kHz - frequency19kHz) * freqResolutionIntervalInverse;
            freqLowerBoundDiscrete = freq19kDiscrete;
            freqUpperBoundDiscrete = freq22kDiscrete;
            detectorBoxFreqUpperBoundDiscrete = freq22kDiscrete - freq19kDiscrete;
        }
        chirpTime2FreqRatio = chirpTimeFraction / chirpFreqFraction;

        int chirpPositiveSlopeTimeLowerBoundDiscrete = detectorBoxTimeLowerBoundDiscrete - chirpTimeDiscrete;
        int chirpPositiveSlopeTimeUpperBoundDiscrete = detectorBoxTimeUpperBoundDiscrete;
        int chirpNegativeSlopeTimeLowerBoundDiscrete = detectorBoxTimeLowerBoundDiscrete;
        int chirpNegativeSlopeTimeUpperBoundDiscrete = detectorBoxTimeUpperBoundDiscrete + chirpTimeDiscrete;

        double chirpPositiveSlopeTimeLowerBoundContinuous = detectorBoxTimeLowerBoundDiscrete - chirpTime2FreqRatio * detectorBoxFreqUpperBoundDiscrete;
        double chirpNegativeSlopeTimeLowerBoundContinuous = detectorBoxTimeLowerBoundDiscrete;
        double mMinSpectrogram = Double.MAX_VALUE;
        double mMaxSpectrogram = Double.MIN_VALUE;

        final double EPSILON = 2.2204e-16;
        double[][] tTransposeDS = new double[timeResolutionDiscrete][freqResolutionDiscrete];
        for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionDiscrete; tColumn++) {
                double tElement = timeFrequenceMatrix.get(tRow)[tColumn];
                tTransposeDS[tRow][tColumn] = Math.abs(10.0 * Math.log10(Math.abs(tElement) + EPSILON));
                if (tTransposeDS[tRow][tColumn] < mMinSpectrogram) {
                    mMinSpectrogram = tTransposeDS[tRow][tColumn];
                }
                if (tTransposeDS[tRow][tColumn] > mMaxSpectrogram) {
                    mMaxSpectrogram = tTransposeDS[tRow][tColumn];
                }
            }
        }

        double[][] tFlipudDS = new double[timeResolutionDiscrete][freqResolutionDiscrete];
        for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionDiscrete; tColumn++) {
                tFlipudDS[tRow][tColumn] = tTransposeDS[timeResolutionDiscrete - tRow - 1][tColumn];
            }
        }

        double mDetSpectrogram = 255.0 / (mMaxSpectrogram - mMinSpectrogram);
        double[][] tNormalizedDS = new double[timeResolutionDiscrete][freqResolutionDiscrete];
        for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
            for (int tColumn = 0; tColumn < freqResolutionDiscrete; tColumn++) {
                tNormalizedDS[tRow][tColumn] = (tFlipudDS[tRow][tColumn] - mMinSpectrogram) * mDetSpectrogram;
            }
        }

        // TODO: whether detectorFreqDiscrete should + 1
        int detectorFreqDiscrete = freqUpperBoundDiscrete - freqLowerBoundDiscrete;
        double[][] tFlipudDSSlice = new double[timeResolutionDiscrete][detectorFreqDiscrete];
        for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
            for (int tColumn = 0; tColumn < detectorFreqDiscrete; tColumn++) {
                tFlipudDSSlice[tRow][tColumn] = tNormalizedDS[tRow][freqLowerBoundDiscrete - 1 + tColumn];
            }
        }

        //
        // Positive slope chirp detector
        //
        int minTimeIntercept = chirpPositiveSlopeTimeLowerBoundDiscrete;
        int maxTimeIntercept = chirpPositiveSlopeTimeUpperBoundDiscrete;
        int detectorTimeDiscrete = maxTimeIntercept - minTimeIntercept + 1;
        double[][] tSpectroPositiveSta = new double[detectorTimeDiscrete][4];
        for (int tRow = 0; tRow < detectorTimeDiscrete; tRow++) {
            for (int tColumn = 0; tColumn < 4; tColumn++) {
                tSpectroPositiveSta[tRow][tColumn] = 0.0;
            }
        }
        double divisorInv = 1.0 / Math.sqrt(1.0 + chirpTime2FreqRatio * chirpTime2FreqRatio);
        for (int timeIntercept = minTimeIntercept; timeIntercept <= maxTimeIntercept; timeIntercept++) {
            for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
                for (int tColumn = 0; tColumn < detectorFreqDiscrete; tColumn++) {
                    double tDistance = Math.abs((timeResolutionDiscrete - (tRow + 1)) - ((tColumn + 1) - 1) * chirpTime2FreqRatio - timeIntercept) * divisorInv;
                    if (tDistance < bufferThreshold) {
                        tSpectroPositiveSta[timeIntercept - minTimeIntercept][0] += tFlipudDSSlice[tRow][tColumn];
                        tSpectroPositiveSta[timeIntercept - minTimeIntercept][1] += 1.0;
                        tSpectroPositiveSta[timeIntercept - minTimeIntercept][2] = tSpectroPositiveSta[timeIntercept - minTimeIntercept][0] / tSpectroPositiveSta[timeIntercept - minTimeIntercept][1];
                    }
//                    Log.i(TAG, "tDistance: " + tDistance);
                }
            }
        }
        for (int i = 0; i < maxTimeIntercept - minTimeIntercept; i++) {
            tSpectroPositiveSta[i][3] = Math.abs(tSpectroPositiveSta[i + 1][2] - tSpectroPositiveSta[i][2]);
        }
        int minSkipTimeIntercept = 1 + skipThreshold;
        int maxSkipTimeIntercept = detectorTimeDiscrete - skipThreshold;
        int skipTimeDiscrete = maxSkipTimeIntercept - minSkipTimeIntercept + 1;
        double[] tSpectroPositiveDet = new double[skipTimeDiscrete];
        for (int i = 0; i < skipTimeDiscrete; i++) {
            tSpectroPositiveDet[i] = tSpectroPositiveSta[i + minSkipTimeIntercept - 1][3];
        }


        //
        // Negative slope chirp detector
        //
        minTimeIntercept = chirpNegativeSlopeTimeLowerBoundDiscrete;
        maxTimeIntercept = chirpNegativeSlopeTimeUpperBoundDiscrete;
        detectorTimeDiscrete = maxTimeIntercept - minTimeIntercept + 1;
        double[][] tSpectroNegativeSta = new double[detectorTimeDiscrete][4];
        for (int tRow = 0; tRow < maxTimeIntercept - minTimeIntercept + 1; tRow++) {
            for (int tColumn = 0; tColumn < 4; tColumn++) {
                tSpectroNegativeSta[tRow][tColumn] = 0.0;
            }
        }
        for (int timeIntercept = minTimeIntercept; timeIntercept <= maxTimeIntercept; timeIntercept++) {
            for (int tRow = 0; tRow < timeResolutionDiscrete; tRow++) {
                for (int tColumn = 0; tColumn < detectorFreqDiscrete; tColumn++) {
                    double tDistance = Math.abs((timeResolutionDiscrete - (tRow + 1)) + ((tColumn + 1) - 1) * chirpTime2FreqRatio - timeIntercept) * divisorInv;
                    if (tDistance < bufferThreshold) {
                        tSpectroNegativeSta[timeIntercept - minTimeIntercept][0] += tFlipudDSSlice[tRow][tColumn];
                        tSpectroNegativeSta[timeIntercept - minTimeIntercept][1] += 1.0;
                        tSpectroNegativeSta[timeIntercept - minTimeIntercept][2] = tSpectroNegativeSta[timeIntercept - minTimeIntercept][0] / tSpectroNegativeSta[timeIntercept - minTimeIntercept][1];
                    }
//                    Log.i(TAG, "tDistance: " + tDistance);
                }
            }
        }
        for (int i = 0; i < maxTimeIntercept - minTimeIntercept; i++) {
            tSpectroNegativeSta[i][3] = Math.abs(tSpectroNegativeSta[i + 1][2] - tSpectroNegativeSta[i][2]);
        }
        double[] tSpectroNegativeDet = new double[skipTimeDiscrete];
        for (int i = 0; i < skipTimeDiscrete; i++) {
            tSpectroNegativeDet[i] = tSpectroNegativeSta[i + minSkipTimeIntercept - 1][3];
        }

        //
        //
        //
        double tMaxSpectroPositiveDet = 0.0;
        int tMaxSpectroPositiveDetInd = 0;
        for (int tCounter = 0; tCounter < maxTimeIntercept - minTimeIntercept; tCounter++) {
            if (tSpectroPositiveSta[tCounter][3] > tMaxSpectroPositiveDet) {
                tMaxSpectroPositiveDet = tSpectroPositiveSta[tCounter][3];
                tMaxSpectroPositiveDetInd = tCounter;
            }
        }

        double tMaxSpectroPositiveDetPart = 0.0;
        int tMaxSpectroPositiveDetIndPart = 0;
        for (int tCounter = 0; tCounter < skipTimeDiscrete; tCounter++) {
            if (tSpectroPositiveDet[tCounter] > tMaxSpectroPositiveDetPart) {
                tMaxSpectroPositiveDetPart = tSpectroPositiveDet[tCounter];
                tMaxSpectroPositiveDetIndPart = tCounter;
            }
        }


        double tMaxSpectroNegativeDet = 0.0;
        int tMaxSpectroNegativeDetInd = 0;
        for (int tCounter = 0; tCounter < maxTimeIntercept - minTimeIntercept; tCounter++) {
            if (tSpectroNegativeSta[tCounter][3] > tMaxSpectroNegativeDet) {
                tMaxSpectroNegativeDet = tSpectroNegativeSta[tCounter][3];
                tMaxSpectroNegativeDetInd = tCounter;
            }
        }

        double tMaxSpectroNegativeDetPart = 0.0;
        int tMaxSpectroNegativeDetIndPart = 0;
        for (int tCounter = 0; tCounter < skipTimeDiscrete; tCounter++) {
            if (tSpectroNegativeDet[tCounter] > tMaxSpectroNegativeDetPart) {
                tMaxSpectroNegativeDetPart = tSpectroNegativeDet[tCounter];
                tMaxSpectroNegativeDetIndPart = tCounter;
            }
        }


        // Output detector result
        int detectorIndex = 0;
        int tClass = -1;
        double tInterceptEst = -128.0;
        if (tMaxSpectroPositiveDetPart < gradientThreshold && tMaxSpectroNegativeDetPart < gradientThreshold) {
            tClass = 0;
            tInterceptEst = 0.0;
        } else if (tMaxSpectroNegativeDetPart > tMaxSpectroPositiveDetPart) {
            if (tMaxSpectroNegativeDet == tMaxSpectroNegativeDetPart) {
                detectorIndex = tMaxSpectroNegativeDetInd;
            } else {
                detectorIndex = minSkipTimeIntercept + tMaxSpectroNegativeDetIndPart - 1;
            }
            tInterceptEst = detectorIndex + 1 + chirpNegativeSlopeTimeLowerBoundContinuous + chirpTime2FreqRatio * freqLowerBoundDiscrete;
            if (frequenceBandFlag == 1) {
                tClass = 1;
            } else if (frequenceBandFlag == 2) {
                tClass = 3;
            }
        } else if (tMaxSpectroNegativeDetPart < tMaxSpectroPositiveDetPart) {
            if (tMaxSpectroPositiveDet == tMaxSpectroPositiveDetPart) {
                detectorIndex = tMaxSpectroPositiveDetInd;
            } else {
                detectorIndex = minSkipTimeIntercept + tMaxSpectroPositiveDetIndPart - 1;
            }
            tInterceptEst = detectorIndex + 1 + chirpPositiveSlopeTimeLowerBoundContinuous - chirpTime2FreqRatio * freqLowerBoundDiscrete;
            if (frequenceBandFlag == 1) {
                tClass = 2;
            } else if (frequenceBandFlag == 2) {
                tClass = 4;
            }
        }

        long tTimeDuration = System.currentTimeMillis() - timeCounter;

        Log.i(TAG, "Estimation: " + tInterceptEst + ":" + tClass);
        Log.d(TAG, "CalculationDuration: " + tTimeDuration);

        return new double[]{tClass, tInterceptEst};
    }

    private double[] calculateTimestamp(final int windowIndex, final double intercept, final int chirpFlag) {

        // Chirp Features
        final double chirpLength = 0.045;
        final double frequency15kHz = 15000.0;
        final double frequency18kHz = 18000.0;
        final double frequency19kHz = 19000.0;

        // STFT Parameters
        final int fftWindowLength = 4096;
        final int fftLength = 1024;
        final int hopLength = 128;
        final double sampleRate = 44100.0;
        final double timeResolutionInterval = hopLength / sampleRate;
        final double freqResolutionIntervalInverse = fftLength / sampleRate;

        final int freq15kDiscrete = (int) Math.round(frequency15kHz * freqResolutionIntervalInverse) + 1;    // 348 + 1
        final int freq19kDiscrete = (int) Math.round(frequency19kHz * freqResolutionIntervalInverse) + 1;    // 441 + 1

        double chirpTimeFraction = chirpLength / timeResolutionInterval;
        double chirpFreqFraction = -1.0;
        double chirpTime2FreqRatio;

        chirpFreqFraction = (frequency18kHz - frequency15kHz) * freqResolutionIntervalInverse;
        chirpTime2FreqRatio = chirpTimeFraction / chirpFreqFraction;

        double m0TimeAxis = (fftLength / 2) / sampleRate;
        double rTimeStamp = 0.0;

        double chirpDelayRefWindow;
        double windowDelayRefTimeAxis = (((windowIndex - 1) * (fftLength * 4 - 896 - 128 * 9)) + 1) / sampleRate;

        switch (chirpFlag) {
            case 1:
                chirpDelayRefWindow = ((-1) * chirpTime2FreqRatio * freq15kDiscrete + intercept) * timeResolutionInterval + m0TimeAxis;
                rTimeStamp = windowDelayRefTimeAxis + chirpDelayRefWindow - chirpLength;
                break;
            case 2:
                chirpDelayRefWindow = ((+1) * chirpTime2FreqRatio * freq15kDiscrete + intercept) * timeResolutionInterval + m0TimeAxis;
                rTimeStamp = windowDelayRefTimeAxis + chirpDelayRefWindow;
                break;
            case 3:
                chirpDelayRefWindow = ((-1) * chirpTime2FreqRatio * freq19kDiscrete + intercept) * timeResolutionInterval + m0TimeAxis;
                rTimeStamp = windowDelayRefTimeAxis + chirpDelayRefWindow - chirpLength;
                break;
            case 4:
                chirpDelayRefWindow = ((+1) * chirpTime2FreqRatio * freq19kDiscrete + intercept) * timeResolutionInterval + m0TimeAxis;
                rTimeStamp = windowDelayRefTimeAxis + chirpDelayRefWindow;
                break;
        }

        Log.i(TAG, "EstimationTimeStamp: " + rTimeStamp + ":" + chirpFlag);

        return new double[]{rTimeStamp, -1.0};
    }

}
