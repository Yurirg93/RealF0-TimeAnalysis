package com.example.yurirodriguez.real_timeanalysis;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    public Button btn = null;
    public Button btnstop = null;
    public TextView textview = null;


    GraphView Graph;
    double q;

    LineGraphSeries<DataPoint> series;

    //Variables de RealTime-A
    int audioSource = MediaRecorder.AudioSource.MIC;    // Audio source is the device MIC
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;    // Recording in mono
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // Records in 16bit
    int sampleRate = 8000;                             // Sample rate in Hz
    int bufferSize;
    private static final String LOG_TAG = "MainActivity" ;
    boolean mShouldContinue; // Indicates if recording / playback should stop
    double[] medfilter = new double[3];
    double returned = 0;
    boolean iniciado = false;
    private Context context;
    double Thr = 0;



    // Vartiables de permisos.
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};


    //----------------------------------------------------------------------------------------------

    @Override  // petición de permisos para poder grabar en el teléfono, storage y grabación

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) MainActivity.super.finish();
        if (!permissionToWriteAccepted ) MainActivity.super.finish();

    }





    //-----------------------------------------------------------------------------------------------

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Graph = (GraphView) findViewById(R.id.graph);

        context = getApplicationContext();
/*        Typeface mytapeface1 = Typeface.createFromAsset(getAssets(),"champagne_bold.ttf");
        Typeface mytapeface2 = Typeface.createFromAsset(getAssets(),"Timeless.ttf");*/



        btn = (Button) findViewById(R.id.btn);            // relacionamos nuestra variable local btn con la view(botón,widget) de nuestro archivo xml.
        btnstop = (Button) findViewById(R.id.btnstop);
        textview = (TextView) findViewById(R.id.textView);

/*        btn.setTypeface(mytapeface1);
        btnstop.setTypeface(mytapeface1);*/

                                                         // petición de permisos para poder grabar en el teléfono
        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }



        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordAudio();

            }
        });


        btnstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShouldContinue = false;
            }


        });


    }



    void recordAudio() {

        mShouldContinue= true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                    handleHeadphonesState(context);
                    // buffer size in bytes
                    bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding); //Consigue el minimo tamaño de buffer para poder analizar

                    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                        bufferSize = 3000; // si el buffersize obtenido por nuestro getMinBufferSize es apto  usaremos el encontrado si no utilizaremos el doble de nuestra frecuencia de sampleo
                    }
                    bufferSize = 2250;
                    short[] audioBuffer = new short[bufferSize];

                    if (audioBuffer.length % 2 == 0){ // Aseguramos que nuestro buffer input tenga una tamaño impar para una mejor R.F.
                        audioBuffer = new short[bufferSize +1];
                    }


                    AudioRecord record = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, bufferSize); //Instancia de la clase AudioRecord

                    if (record.getState() != AudioRecord.STATE_INITIALIZED) { // Si audiorecord no ha sido inicializado displeamos un mensaje advirtiendo.
                        Log.e(LOG_TAG, "Audio Record can't initialize!");
                        return;
                    }
                    record.startRecording(); //Empezamos a grabar con nuestros parámetros ya definidos.

                    Log.v(LOG_TAG, "Start recording");  //mensaje informativo

                    long shortsRead = 0;

                    while (mShouldContinue) {





                    for (int i =0; i<=medfilter.length ; i++){

                            if (i<3){
                            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length); //Reading Data
                            shortsRead += numberOfShort;}

                            if (i == 3){
                                Log.v(LOG_TAG, "med " + medfilter[1]);
                                Medfilter();
                                iniciado = true;
                                i = 0;
                                break;
                            }

                            DFT(audioBuffer);

/*                            try { // Dormimos el programa durante un segundo
                                Thread.sleep(200);
                            } catch(InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }*/


                            if (iniciado == false) {
                                medfilter[i] = returned;
                                returned = 0;
                            }
                            else{
                                medfilter[2] = returned;
                                i = 2;
                            }

                    }





                    }

                        record.stop();
                        record.release();
                        runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textview.setText("---");                      // Hacemos update del texto en pantalla
                            Graph.removeAllSeries();
                        }
                        });


                        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));




                }
            }).start();
        }



    public double[] DFT(short[] InputSignal){

        // VARIABLES
        /////////////////////////////////////////////////////

        int N = InputSignal.length;
        double[] doubley = new double[N];
        double[] Lowdoubley = new double[N];
        double[] W;
        int Nfft = 16384;
        double temp;
        double pos;
/*
        double Valpos;
*/
        double MaxPeak = 0;
/*
        int nPeak = 0;
*/
        double freq = 0;
        double freqMax = 0.01;
        Complex[] M;
        Complex[] complexSignal = new Complex[Nfft];
        double[] absSignal = new double[(Nfft/2)+1];
        double[] dBabsSignal = new double[(Nfft/2)+1];
        double[] zeropadding = new double[Nfft];
        double[] PeakArray = new double[(Nfft/2)+1];
        int j = 0;
/*
        double max = 0;
*/
        /////////////////////////////////////////////////////


        // PROCESADO
        ////////////////////////////////////////////////////
/*        for(int i = 0; i < N; i++){
            if (Math.abs(InputSignal[i]) > max) {
                max = Math.abs(InputSignal[i]);
            }
        }*/


        for(int i = 0; i < N; i++){
            doubley[i] = (double)(InputSignal[i])/100.0; // pasamos nuestro vector de short a byte
        }

        lowPass(doubley,Lowdoubley); // Low passing data

       for(int i = 0 ; i < Nfft; i++){              //Aplicamos zero-padding antes de realizar nnuestra ventana ( si aplico zero padding al final MALOS RESULTADOS)
            if((i>=((Nfft-N)/2)) && (i<(N+((Nfft-N)/2)))){

                zeropadding[i] = Lowdoubley[j];
                j = j+1;
                }
            else{

                zeropadding[i] =0;}
        }


        W = HammingWindow(zeropadding,0,N);                    // Aplicamos ventana.


        //PASAMOS NUESTRO VECTOR A TIPO COMPLEJO

        for(int i = 0; i < Nfft; i++){
            temp = W[i];
            complexSignal[i] = new Complex(temp,0.0); // Pasamos nuestro vector de muestras de double to complex para realizar la FFT
        }

        //APlICAMOS FFT
        M = FFT.fft(complexSignal);                     // --> FFT class




        // CALCULAMOS MAGNITUD Y REDONDEAMOS
        for(int i = 0; i < (Nfft/2)+1; i++)              // mX y mXdB
        {
            absSignal[i] = Math.sqrt(Math.pow(M[i].re(), 2) + Math.pow(M[i].im(), 2)); //mX de FFT, mitad de puntos (parte positiva).
            if (Thr == 4){
                dBabsSignal[i] = 20*Math.log10(absSignal[i]/10);
/*
                dBabsSignal[i] = 2*dBabsSignal[i];
*/

            }
            else{
                dBabsSignal[i] = 20*Math.log10(absSignal[i]/100);

            }
            if (dBabsSignal[i]< -40){
                dBabsSignal[i] = -40;} // limite inferior
        }

/*        //Low Pass filter fc = 1080 Hz
        for(int i =2212 ; i <(Nfft/2)+1;i++){
            absSignal[i] = 0;
            dBabsSignal[i] = -20;

        }*/


        double x = 0;
        // CALCULAMOS PICOS y SUB-ARMONICOS
        for ( int i = 1; i < (Nfft/2); i++) {
            //Peak Detection

            if (dBabsSignal[i - 1] < dBabsSignal[i] && dBabsSignal[i] > dBabsSignal[i + 1] && dBabsSignal[i] > Thr) {//Bn'Aft,Thres Con

                PeakArray[i] = dBabsSignal[i];          //Array de picos

                // Suming sub-Harmonics
                x = i;
                if (x / 2 >= 0) {
                    if(x/2 % 1 != 0){
                        if((x/2 % 1) >= 0.5){
                            PeakArray[(i/2) + 1] = PeakArray[(i/2) + 1] + (PeakArray[i]/2);}
                        else{
                            PeakArray[(i/2)] = PeakArray[(i/2)] + (PeakArray[i]/2);
                        }
                    }
                    else{
                        PeakArray[i/2] = PeakArray[i/2] + (PeakArray[i]/2);}
                }
                if (x / 3 >= 0) {
                    if(x/3 % 1 != 0){
                        if((x/3 % 1) >= 0.5){
                            PeakArray[(i/3) + 1] = PeakArray[(i/3) + 1] + (PeakArray[i]/3);}
                        else{
                            PeakArray[(i/3)] = PeakArray[(i/3)] + (PeakArray[i]/3);}

                    }
                    else{
                        PeakArray[i/3] = PeakArray[i / 3] + (PeakArray[i]/(3));}
                }
                if (x/4 >= 0) {
                    if(x/4 % 1 != 0){
                        if((x/4 % 1) >= 0.5){
                            PeakArray[(i/4) + 1] = PeakArray[(i/4) + 1] + (PeakArray[i]/4);}
                        else{
                            PeakArray[(i/4)] = PeakArray[(i/4)] + (PeakArray[i]/4);}
                    }
                    else{
                        PeakArray[i/4] = PeakArray[i / 4] + (PeakArray[i]/(4));}
                }
                if (x/5 >= 0) {
                    if(x/5 % 1 != 0){
                        if((x/5 % 1) >= 0.5){
                            PeakArray[(i/5) + 1] = PeakArray[(i/3) + 1] + (PeakArray[i]/5);}
                        else{
                            PeakArray[(i/5)] = PeakArray[(i/5)] + (PeakArray[i]/5);}
                    }
                    else{
                        PeakArray[i/5] = PeakArray[i/5] + (PeakArray[i]/(5));}
                }

            }

        }
        // Barremos PeakArray
        for(int i = 1; i < (Nfft/2); i++)
        {
            if (PeakArray[i-1] !=0 && PeakArray[i] !=0 && PeakArray[i+1]!=0) {
                PeakArray[i] = 2*PeakArray[i] + (1/4)*PeakArray[i-1] + (1/4)*PeakArray[i+1];
            }
        }
        for(int i = 1; i < (Nfft/2); i++)
        {

            if(PeakArray[i-1] ==0 && PeakArray[i+1] !=0 && PeakArray[i+2] == 0){

                if(PeakArray[i]>PeakArray[i+1]){
                    PeakArray[i] = PeakArray[i]+PeakArray[i+1];}
                else{
                    PeakArray[i+1] = PeakArray[i]+PeakArray[i+1];}
            }
        }




        //ENCOTRAMOS EL PICO DOMINANTE
        for (int i=163; i<2212; i++){
            if(MaxPeak < PeakArray[i]){
                MaxPeak = PeakArray[i];
/*
                nPeak = i;
                Valpos = absSignal[i] - (0.25 * ((absSignal[i - 1] - absSignal[i + 1]) )* (pos - i)); // Magnitud centro parábola*/
            if(i>0){
                pos = i + (0.5 * (absSignal[i - 1] - absSignal[i + 1]) / (absSignal[i - 1] - (2 * (absSignal[i] + absSignal[i + 1])))); //Centro de la parábola
                freq = (pos*sampleRate)/Nfft;
                freq = roundDown2(freq);}
            }
        }

        Ploting(dBabsSignal,Nfft);



        /*Log.v(LOG_TAG, "Max " + MaxPeak);
        Log.v(LOG_TAG, "Max pico " + nPeak);*/
        Log.v(LOG_TAG,"freq " + freq);


        returned = freq;
        return absSignal;

    }





    public double[] HanningWindow(double[] signal_in, int pos, int size)
    {
        for (int i = pos; i < pos + size; i++)
        {
            int j = i - pos; // j = index into Hann window function
            signal_in[i] = (signal_in[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * j / (size))));
        }
        return signal_in;
    }

    public double[] HammingWindow(double[] signal_in, int pos, int size)
    {
        for (int i = pos; i < pos + size; i++)
        {
            int j = i - pos; // j = index into Hamming window function
            signal_in[i] = (signal_in[i] * 0.5 * (1.07672 - (0.92328*Math.cos(2.0 * Math.PI * j / (size)))));
        }
        return signal_in;
    }

    public static void intercambio(double lista[]){


        //Usamos un bucle anidado
        for(int i=0;i<(lista.length-1);i++){
            for(int j=i+1;j<lista.length;j++){
                if(lista[i]>lista[j]){
                    //Intercambiamos valores
                    double variableauxiliar=lista[i];
                    lista[i]=lista[j];
                    lista[j]=variableauxiliar;

                }
            }
        }

    }

    public void ChangeText (final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textview.setText(s + " Hz");                      // Hacemos update del texto en pantalla
            }
        });

    }

    public void Ploting (final double[] s, final int n){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                q = 0;
                series = new LineGraphSeries<DataPoint>();

                for (int i=0; i < n/8;i++){
                    q = (i*sampleRate)/n; // Freq Value
                    series.appendData(new DataPoint(q,s[i]),true,n/8);
                }
                Graph.removeAllSeries();
                series.setColor(Color.rgb(255,250,250));
                Graph.setBackgroundColor(Color.argb(0,0,0,0));
                Graph.addSeries(series);
            }
        });

    }

    void Medfilter() {
        double[] filtro = new double[3];
        for (int i = 0;i<3;i++){
            filtro[i]=medfilter[i];
        }
        intercambio(filtro);
        String s = new Double(filtro[1]).toString();
        ChangeText(s);
        medfilter[0] = medfilter [1];
        medfilter[1] = medfilter [2];
        medfilter[2] = 0;

    }
    public static double roundDown2(double d) {
        return Math.floor(d * 1e2) / 1e2;
    }



    static final double ALPHA = 0.15f;

    protected double[] lowPass( double[] input, double[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
    public void handleHeadphonesState(Context context){
        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        if(am.isWiredHeadsetOn()) {
            Thr = 4;// plugged in
        } else{
           Thr = 5; //  unplugged
        }
    }

}





