package com.example.android.syncx;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import com.example.SyncX.R;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SyncX.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SocketServerPORT = 8080;
    String dstAddress;
    int dstPort;
    static int BUFFER_SIZE = 10240;
    static String REQUEST = "o";
    static String RESPONSE = "r";
    static String PERMANENT = "p";
    static String TEMPORARY = "t";
    static String PACKET_FROM_SERVER= "s";
    static String PACKET_FROM_CLIENT = "c";
    int id_count = 0;
    int file_count = 0;
    List<Uri> jobs=new LinkedList<>();

    DataOutputStream permanent_input;
    Hashtable<Integer, String> id_to_file_to_be_wrote=new Hashtable<>();
    Hashtable<String, Uri> getId_to_file_to_be_send=new Hashtable<>();

    LinearLayout loginPanel;

    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView  textPort;

    Button buttonSend;

    String msgLog = "";
    String textAddress="";
    PermanentClient permanentClient = null;
    Intent myFileIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginPanel = (LinearLayout)findViewById(R.id.loginpanel);
        editTextUserName = (EditText) findViewById(R.id.username);
        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);

        buttonSend  = findViewById(R.id.Send);
        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        intent_handling();

    }





    private class Jobs_completer extends  Thread{

        Jobs_completer(){

        }


        @Override
        public void run() {
            while (!jobs.isEmpty()) {

                Uri file_path = jobs.get(0);
                jobs.remove(0);
                File_sender file_sender = new File_sender(file_path);
                file_sender.start();
                try {
                    file_sender.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }





        }
    }

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override


        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();
            if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Addresse",
                        Toast.LENGTH_LONG).show();
                return;
            }



            dstAddress = textAddress;
            dstPort = SocketServerPORT;

            permanentClient = new PermanentClient(textAddress, SocketServerPORT);
            permanentClient.start();
        }

    };





    ProgressBar inflateProgressBar(int index_y,int size){
        LinearLayout place1=(LinearLayout) findViewById(R.id.main_container);


        while(place1.getChildCount()<index_y){
            getLayoutInflater().inflate(R.layout.container,place1);}



        LinearLayout container =(LinearLayout)place1.getChildAt(index_y-1);

        container.setWeightSum(size);


        getLayoutInflater().inflate(R.layout.progresbar,container);
//        Toast.makeText(this,place1.getChildCount()+"sa",Toast.LENGTH_SHORT).show();


        ProgressBar pbar=(ProgressBar) container.getChildAt(container.getChildCount()-1);

        return pbar;

    }


    public void initiate_send(View view) {
        String textUserName = editTextUserName.getText().toString();
        if (textUserName.equals("")) {
            Toast.makeText(MainActivity.this, "Enter User Name",
                    Toast.LENGTH_LONG).show();
            return;
        }

        textAddress = editTextAddress.getText().toString();
        if (textAddress.equals("")) {
            Toast.makeText(MainActivity.this, "Enter Addresse",
                    Toast.LENGTH_LONG).show();
            return;
        }

        myFileIntent= new Intent(Intent.ACTION_GET_CONTENT);
        myFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        myFileIntent.setType("*/*");
        startActivityForResult(myFileIntent, 10);

    }



    void intent_handling(){
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            {
                Uri single_uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                jobs.add(single_uri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            {

                ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                if (imageUris != null) {
                    jobs.addAll(imageUris);
                }

            }
        } else {


            // Handle other intents, such as being started from the home screen
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (requestCode==10){

            if(resultCode==RESULT_OK){
                try {

                    ClipData clipData = data.getClipData();
                    Log.d("nubmer",Integer.toString(clipData.getItemCount()));
                    for(int i = 0 ;i<clipData.getItemCount();i++) {





                        Uri file_path = clipData.getItemAt(i).getUri();





                        File_sender file_sender = new File_sender(file_path);
                        file_sender.start();
                        file_sender.join();

                    }
                }

                catch (Exception e) {
                    Uri file_path = data.getData();

                    File_sender fileSenderThread = new File_sender(file_path);
                    fileSenderThread.start();
                    e.printStackTrace();
                }

            }


        }

    }

    private String displayName(Uri uri) {

        Cursor mCursor =
                getApplicationContext().getContentResolver().query(uri, null, null, null, null);
        int indexedname = mCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        mCursor.moveToFirst();
        String filename = mCursor.getString(indexedname);
        mCursor.close();
        return filename;
    }

    private class File_sender extends Thread {

        Uri sending_file_path;
        long file_size;
        String file_name ;

        File_sender(Uri sending_file_path) {
            this.sending_file_path = sending_file_path;
        }

        @Override
        public void run() {
            DataOutputStream dataOutputStream = permanent_input;


            try {



                ParcelFileDescriptor file = getContentResolver().openFileDescriptor(sending_file_path,"r");
                file_size=file.getStatSize();

                file_name = displayName(sending_file_path);

                Log.d("file_name",file_name);

                int file_name_length = file_name.length();

                file.close();

                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(file_name_length);
                byte[] name_size_bytes = b.array();



                ByteBuffer bb = ByteBuffer.allocate(8);
                bb.putLong(file_size);
                byte[] file_size_bytes = bb.array();

                Log.d("second_error","file_size"+file_size+" name"+file_name+" id " + id_count);



                dataOutputStream.writeBytes(REQUEST);
                dataOutputStream.write(name_size_bytes);
                dataOutputStream.writeBytes(file_name);
                dataOutputStream.write(file_size_bytes);

                //just junk values
                dataOutputStream.write(name_size_bytes);
                dataOutputStream.write(name_size_bytes);



                getId_to_file_to_be_send.put(file_name,sending_file_path);









            } catch (UnknownHostException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }

        }


    }



    private class Send_packet extends Thread {

        long starting_point;
        long file_size;
        int data_id;
        int no_of_sockets;

        Uri sending_file_path;
        int buffer_size = BUFFER_SIZE;
        long remaining_size;
        ProgressBar progressBar;
        int file_percentage;
        int file_counting;

        Send_packet(Uri  uri_id, long starting_point , long file_size, int id,int no_of_sockets, int file_counti) {

            this.data_id = id;
            this.sending_file_path = uri_id;
            this.starting_point = starting_point;
            this.file_size = file_size;
            this.no_of_sockets = no_of_sockets;
            this.file_counting  = file_counti;


        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;



            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());


                dataOutputStream.writeBytes(TEMPORARY);

                dataOutputStream.writeBytes(PACKET_FROM_CLIENT);
                Log.d("second_error",TEMPORARY+" "+PACKET_FROM_CLIENT);
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(data_id);
                byte[] data_buffer_id = b.array();


                ByteBuffer b1 = ByteBuffer.allocate(8);
                Log.d("second_error", "Starting point is "+starting_point);
                b1.putLong(starting_point);
                byte[] starting_point_buffer = b1.array();

                ByteBuffer b2 = ByteBuffer.allocate(8);
                b2.putLong(file_size);
                byte[] file_size_buffer = b2.array();

                dataOutputStream.write(data_buffer_id);
                dataOutputStream.write(starting_point_buffer);
                dataOutputStream.write(file_size_buffer);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        progressBar = inflateProgressBar(file_counting,no_of_sockets);
                        Log.d("sender",no_of_sockets+" "+file_counting);

                    }

                });







                FileInputStream fis = (FileInputStream)getContentResolver().openInputStream(sending_file_path);



                remaining_size = file_size;

                fis.skip(starting_point);



                while(remaining_size>0) {

                    if (remaining_size<buffer_size){
                        buffer_size = (int)remaining_size;
                    }

                    byte[] buffer = new byte[buffer_size];
                    try {
                        //reading from file
                        fis.read(buffer);


                        //sending to tcp stream
                        dataOutputStream.write(buffer);
                        dataOutputStream.flush();


                        remaining_size = remaining_size - buffer_size;
                        file_percentage = (int) (((float)(file_size - remaining_size)/(float)file_size)*100.0);


                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
//                                msg//Log = "";
                                //            chatMsg.setText(msg//Log);
//                                loginPanel.setVisibility(View.GONE);
//                                chatPanel.setVisibility(View.VISIBLE);

                                progressBar.setProgress(file_percentage);

                            }

                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }




            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }



            }

        }


    }




    private class Recieve_packet extends Thread {

        Socket client;
        int data_id;
        long starting_point;
        long file_size;
        int no_of_sockets;
        int file_counting;



        String name_of_file;
        int file_percentage =0;
        ProgressBar progressBar ;
        File myExternal_file ;
        int buffer_size = BUFFER_SIZE;


        Recieve_packet(int id, long starting_point, long file_size,int no_of_sockets,int file_counti) {
            this.data_id = id;
            this.starting_point = starting_point;
            this.file_size = file_size;
            this.no_of_sockets = no_of_sockets;
            this.file_counting = file_counti;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataInputStream dataInputStream = null;



            try {

                socket = new Socket(dstAddress, dstPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeBytes(TEMPORARY);
                dataOutputStream.writeBytes((PACKET_FROM_SERVER));

                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(data_id);
                byte[] data_id_buffer = b.array();


                ByteBuffer b1 = ByteBuffer.allocate(8);
                Log.d("my_error","starting_point is "+starting_point+","+file_size+","+data_id);
                b1.putLong(starting_point);
                byte[] starting_point_buffer = b1.array();

                ByteBuffer b2 = ByteBuffer.allocate(8);
                b2.putLong(file_size);
                byte[] file_size_buffer = b2.array();


                dataOutputStream.write(data_id_buffer);
                dataOutputStream.write(starting_point_buffer);
                dataOutputStream.write(file_size_buffer);

                byte[] temp_buffer = new byte[20] ;

                dataInputStream.readFully(temp_buffer);




                name_of_file = id_to_file_to_be_wrote.get(data_id);

                myExternal_file = new File(getExternalFilesDir(null),name_of_file);

                if(!myExternal_file.exists()){

                    myExternal_file.createNewFile();
                }


                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        progressBar = inflateProgressBar(file_counting,no_of_sockets);
                        Log.d("sender",no_of_sockets+" "+file_count);


                    }

                });


                RandomAccessFile raf = new RandomAccessFile(myExternal_file,"rw");

                raf.seek(starting_point);





                long remaining_file = file_size;



                while (remaining_file>0) {

                    int offset ;
                    if (remaining_file<buffer_size){
                        offset =  (int)remaining_file;

                    }else{
                        offset = buffer_size;
                    }


                    byte[] buffer = new byte[offset];
                    int k =dataInputStream.read(buffer);




                    raf.write(buffer,0,k);
                    remaining_file = remaining_file - k;
                    file_percentage = (int) (((float)(file_size - remaining_file)/(float)file_size)*100.0);
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            msgLog = "";

                            progressBar.setProgress(file_percentage);

                        }

                    });



                }

                raf.close();


                progressBar.setProgress(100);


            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }


                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }


            }

        }

    }







    private class PermanentClient extends Thread{
        String acknowledgement=PERMANENT;
        String dstAddress;
        int dstPort;


        PermanentClient(String address, int port) {
            this.dstAddress = address;
            this.dstPort = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                permanent_input = dataOutputStream;
                dataOutputStream.writeBytes(acknowledgement);




                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonSend.setVisibility(View.VISIBLE);
                        buttonConnect.setText("CONNECTED");
                        buttonConnect.setEnabled(false);

                    }
                });

                Jobs_completer jobs_completer = new Jobs_completer();
                jobs_completer.start();



                while (true) {
                    byte[] buffer = new byte[1];
                    dataInputStream.read(buffer);
                    String message_type = new String(buffer);


                    if(message_type.equals(RESPONSE)){

                        byte[] buffer1 = new byte[4];
                        //reading name size
                        dataInputStream.readFully(buffer1);
                        int name_size  = ByteBuffer.wrap(buffer1).getInt();

                        //reading name of the file
                        byte[] name_buffer = new byte[name_size];
                        dataInputStream.readFully(name_buffer);
                        String name = new String(name_buffer);

                        //reading file size
                        byte[] file_size_buffer = new byte[8];
                        dataInputStream.readFully(file_size_buffer);
                        long file_size = ByteBuffer.wrap(file_size_buffer).getLong();

                        //reading id
                        dataInputStream.readFully(buffer1);
                        int id = ByteBuffer.wrap(buffer1).getInt();;

                        //reading number_of_sockets
                        dataInputStream.readFully(buffer1);
                        final int number_of_sockets= ByteBuffer.wrap(buffer1).getInt();


                        //adding he value into hash
                        Uri uri_id = getId_to_file_to_be_send.get(name);
                        Log.d("second_error","finally came to size calculation");
                        //ceil

                        file_count++;
                        long data_length = file_size/number_of_sockets;

                        for (int k = 0;k<number_of_sockets-1;k++){



                            //todo
                            long starting_point  = k*data_length;

                            Send_packet packet_sender = new Send_packet(uri_id, starting_point,data_length,id,number_of_sockets,file_count);
                            packet_sender.start();

                            //start a send_packet socket thread
                        }




                        long remaining_data = file_size - data_length*(number_of_sockets-1);
                        Send_packet packet_sender = new Send_packet(uri_id, data_length*(number_of_sockets-1),remaining_data,id,number_of_sockets,file_count);
                        packet_sender.start();




                    }else if(message_type.equals(REQUEST)){

                        Log.d("my_error","entering handling request mode");


                        byte[] name_size_bytes = new byte[4];
                        //reading name size
                        dataInputStream.readFully(name_size_bytes);
                        int name_size  = ByteBuffer.wrap(name_size_bytes).getInt();

                        //reading name of the file
                        byte[] name_buffer = new byte[name_size];
                        dataInputStream.readFully(name_buffer);
                        String name = new String(name_buffer);

                        //reading file size
                        byte[] file_size_buffer = new byte[8];
                        dataInputStream.readFully(file_size_buffer);
                        long file_size = ByteBuffer.wrap(file_size_buffer).getLong();


                        //reading id
                        byte[] id_bytes = new byte[4];
                        dataInputStream.readFully(id_bytes);

                        //reading number_of_sockets
                        byte[] socket_bytes = new byte[4];
                        dataInputStream.readFully(socket_bytes);

                        final int number_of_sockets = ByteBuffer.wrap(socket_bytes).getInt();

                        Log.d("my_error",id_count+name);

                        id_to_file_to_be_wrote.put(id_count,name);
                        id_count++;

                        ByteBuffer id = ByteBuffer.allocate(4);
                        id.putInt(id_count-1);
                        byte[] id_buffer = id.array();


                        dataOutputStream.writeBytes(RESPONSE);
                        dataOutputStream.write(name_size_bytes);
                        dataOutputStream.write(name_buffer);
                        dataOutputStream.write(file_size_buffer);
                        dataOutputStream.write(id_buffer);
                        dataOutputStream.write(socket_bytes);

                        long data_length = file_size/number_of_sockets;
                        file_count++;


                        for (int k = 0;k<number_of_sockets-1;k++){



                            long starting_point  = k*data_length;


                            Recieve_packet packet_sender = new Recieve_packet(id_count-1, starting_point,data_length,number_of_sockets,file_count);
                            packet_sender.start();

                            //start a send_packet socket thread
                        }

                        long remaining_data = file_size - data_length*(number_of_sockets-1);
                        Recieve_packet packet_sender = new Recieve_packet(id_count-1, data_length*(number_of_sockets-1),remaining_data,number_of_sockets,file_count);
                        packet_sender.start();





                    }else{
                        Log.d("my_error",message_type);

                    }



                }



            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buttonSend.setVisibility(View.GONE);
                            buttonConnect.setText("CONNECT");
                            buttonConnect.setEnabled(true);

                        }
                    });
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }

        }


    }

}




