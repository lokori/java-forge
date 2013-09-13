package foo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * Early attempt to create a "real" demo effect + some sound in 4k with Java.
 * <p>
 * 
 * Sort of works. Unreleased skit from 2010.
 *
 * @author Antti "lokori" Virtanen
 */
public final class Effecto implements ActionListener {

    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;
    private byte audioData[];

    private SimpleUniverse mUniverse;
    private Canvas3D mCanvas;

    private TransformGroup objTrans;
    private Transform3D trans = new Transform3D();
    private float height=0.0f;
    private float sign = 1.0f; // going up or down
    private Timer timer;
    private float xloc=0.0f;

    private void generateAudioData() {
        int sampleSize = 20000;
        byte b = Byte.MIN_VALUE;
        int stepper = -1;
        audioData = new byte[sampleSize];
        for (int i=0; i<sampleSize; ++i) {
            audioData[i] = b;
            if (((int)b + stepper) >= (int)Byte.MAX_VALUE) {
                stepper = -stepper;
            } else if (((int)b - stepper) <= (int)Byte.MIN_VALUE) {
                stepper = -stepper;
            }
            b += stepper;
        }
    }

    private BranchGroup createSceneGraph() {

        // Create the root of the branch graph
        BranchGroup objRoot = new BranchGroup();
        // Create a simple shape leaf node, add it to the scene graph.

        objTrans = new TransformGroup();
        Sphere sphere = new Sphere(0.25f);
        objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Transform3D pos1 = new Transform3D();
        pos1.setTranslation(new Vector3f(0.0f,0.0f,0.0f));
        objTrans.setTransform(pos1);
        objTrans.addChild(sphere);
        
        objRoot.addChild(objTrans);

        BoundingSphere bounds =
            new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);

        Color3f light1Color = new Color3f(1.0f, 0.0f, 0.2f);
        Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
        DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
        light1.setInfluencingBounds(bounds);
        objRoot.addChild(light1);

        // Set up the ambient light
        Color3f ambientColor = new Color3f(1.0f, 1.0f, 1.0f);
        AmbientLight ambientLightNode = new AmbientLight(ambientColor);
        ambientLightNode.setInfluencingBounds(bounds);
        objRoot.addChild(ambientLightNode);

        return objRoot;

    }
    
    public void actionPerformed(ActionEvent e ) {
        height += .05 * sign;
        if (Math.abs(height *2) >= 1 ) sign = -1.0f * sign;
        if (height<-0.4f) {
            trans.setScale(new Vector3d(1.0, .8, 1.0));
        } else {
            trans.setScale(new Vector3d(1.0, 1.0, 1.0));
        }
        trans.setTranslation(new Vector3f(xloc,height,0.0f));
        objTrans.setTransform(trans);
    }

    private Effecto() {
        mCanvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        mUniverse = new SimpleUniverse(mCanvas);
        BranchGroup scene = createSceneGraph();
        mUniverse.getViewingPlatform().setNominalViewingTransform();
        mUniverse.addBranchGraph(scene);

        // Create a frame to display it and set the canvas as the center component
        JFrame frame = new JFrame();
        frame.getContentPane().add(mUniverse.getCanvas(), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.show();

//        System.out.println("starting animation");
        timer = new Timer(20, this);
        timer.start();

        generateAudioData();
//        System.out.println("audio generated ok");
        playAudio();
//        System.out.println("playback running");
    }
    
    private void playAudio() {
        try{
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = getAudioFormat();
            audioInputStream = new AudioInputStream(
                    byteArrayInputStream,
                    audioFormat,
                    audioData.length/audioFormat.getFrameSize());
            DataLine.Info dataLineInfo = new DataLine.Info(
                    SourceDataLine.class,
                    audioFormat);
            sourceDataLine = (SourceDataLine)
            AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            Thread playThread = new Thread(new PlayThread());
            playThread.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    private AudioFormat getAudioFormat(){
        /*
        float sampleRate = 16000.0F; //8000,11025,16000,22050,44100
        int sampleSizeInBits = 16; //8,16
        int channels = 1; //1,2
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
*/
        return new AudioFormat(
                16000.0F,
                16,
                1,
                true,
                false);
    }

    class PlayThread extends Thread{
        byte tempBuffer[] = new byte[10000];

        public void run(){
            try{
                int cnt;
                //Keep looping until the input
                // read method returns -1 for
                // empty stream.
                while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
                    if(cnt > 0){
                        //Write data to the internal
                        // buffer of the data line
                        // where it will be delivered
                        // to the speaker.
                        sourceDataLine.write(tempBuffer, 0, cnt);
                    }
                }
                //Block and wait for internal
                // buffer of the data line to
                // empty.
                sourceDataLine.drain();
                sourceDataLine.close();
            }catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        new Effecto();
    }


    
}
