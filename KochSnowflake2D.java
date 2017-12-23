package koch_snowflake_2d;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;

import javax.swing.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static com.jogamp.opengl.GL4.*;
import static java.lang.Math.sqrt;

public class KochSnowflake2D extends JFrame implements GLEventListener
{	private GLCanvas myCanvas;
    private int rendering_program;
    private int vao[] = new int[1];
    private int vbo[] = new int[2];
    private float cameraX, cameraY, cameraZ;
    private GLSLUtils util = new GLSLUtils();
    private int nSides;
    private int nPoints;
    private float originalSideLength;
    private ArrayList<Float> vertex_positions;
    private int iterations;
    private double originalTriangleArea;
    private double area;
    public KochSnowflake2D(float sideLength, int n)
    {	setTitle("Koch Snowflake 2D");
        setSize(600, 600);
        //Making sure we get a GL4 context for the canvas
        nSides = (int) (3 * Math.pow(4, n));
        nPoints = nSides * 2;
        iterations = n;
        originalSideLength = sideLength;
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
        myCanvas = new GLCanvas(capabilities);
        //end GL4 context
        myCanvas.addGLEventListener(this);
        getContentPane().add(myCanvas);
        this.setVisible(true);
        vertex_positions = new ArrayList<>();
        originalTriangleArea = Math.sqrt(3)/4.0 * Math.pow(sideLength, 2);
        area = originalTriangleArea/5.0 * (8 - (3 * (Math.pow(4.0/9.0, n))));
//        1. the value of n
//        2. the number of segments that make up the snowflake
//        3. the total length of the perimeter
//        4. the total area
        System.out.println("The value of n is: " + n);
        System.out.println("The number of segments is: " + nSides);
        System.out.println("The length of the perimeter is: " + (nSides * sideLength));
        System.out.println("The total area is: " + area);
    }

    public void display(GLAutoDrawable drawable)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glClear(GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(rendering_program);

        int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
        int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        Matrix3D pMat = orthogonal(-1.5f,1.5f,1.5f,-1.5f,0.1f,1000.0f);

        Matrix3D vMat = new Matrix3D();
        vMat.translate(-cameraX, -cameraY, -cameraZ);
        //Just drawing 2D - not moving the object
        Matrix3D mMat = new Matrix3D();
        mMat.setToIdentity();

        Matrix3D mvMat = new Matrix3D();
        mvMat.concatenate(vMat);
        mvMat.concatenate(mMat);

        gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
        gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);

        gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);//We are only passing two components
        gl.glEnableVertexAttribArray(0);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        //gl.glPointSize(6.0f);

        gl.glDrawArrays(GL_LINES, 0, nPoints);
    }

    public void init(GLAutoDrawable drawable)
    {	GL4 gl = (GL4) drawable.getGL();
        rendering_program = createShaderProgram();
        setupVertices(iterations);
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 3.0f;
    }

    private void kochCurve(float xStart, float yStart, float xEnd, float yEnd, int n) {

        float aX;
        float aY;
        float bX;
        float bY;
        float cX;
        float cY;
        float dX;
        float dY;
        float eX;
        float eY;


        if (n <= 0) {
            // just draw a line from the start vertex to the end vertex
            // the x and y start coordinates
            vertex_positions.add(xStart);
            vertex_positions.add(yStart);

            // the x and y end coordinates
            vertex_positions.add(xEnd);
            vertex_positions.add(yEnd);

        }
        else {

            // Point A
            // beginning point
            aX = xStart;
            aY = yStart;

            // Point B
            // 2/3 of the way
            bX = (float) ((2.0 * xStart + xEnd) / 3.0);
            bY = (float) ((2.0 * yStart + yEnd) / 3.0);

            // Point C
            // point above halfway mark on the line
            cX = (float) ((xStart + xEnd) / 2.0 - Math.sqrt(3.0) / 6.0 * (yEnd - yStart));
            cY = (float) (( yStart + yEnd ) / 2.0 + Math.sqrt(3.0) / 6.0 * (xEnd - xStart));

            // Point D
            // 1/3 of the way
            dX = (float) ((xStart + 2.0 * xEnd ) / 3.0);
            dY = (float) ((yStart + 2.0 * yEnd) / 3.0);

            // Point E
            // ending point
            eX = xEnd;
            eY = yEnd;

            //line a to b
            kochCurve (aX, aY, bX, bY, n-1);

            //line b to c
            kochCurve (bX, bY, cX, cY, n-1);

            //line c to d
            kochCurve (cX, cY, dX, dY, n-1);

            //line d to e
            kochCurve (dX, dY, eX, eY, n-1);
        }
    }

    private void setupVertices(int n)
    {
        float topX;
        float topY;
        float bottomRightX;
        float bottomRightY;
        float bottomLeftX;
        float bottomLeftY;
        float[] vertices = new float[nPoints*2];

        GL4 gl = (GL4) GLContext.getCurrentGL();
        //The first three vertices define the starting triangle
        //Equilateral triangle centered at the origin
        float side_length = originalSideLength;
        //Top vertex - x and y
        topX = 0;
        topY = side_length*(float) sqrt(3)/3;

        // bottom right vertex - x and y
        bottomRightX = 0.5f*side_length;
        bottomRightY = -(float) sqrt(3)*side_length/6;

        // bottom left vertex - x and y
        bottomLeftX = -0.5f*side_length;
        bottomLeftY = -(float) sqrt(3)*side_length/6;

        kochCurve(topX, topY, bottomRightX, bottomRightY, n);
        kochCurve(bottomRightX, bottomRightY, bottomLeftX, bottomLeftY, n);
        kochCurve(bottomLeftX, bottomLeftY, topX, topY, n);

        for (int i = 0; i < vertex_positions.size(); i++) {
            vertices[i] = vertex_positions.get(i);
        }



        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertices);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
    }

    private Matrix3D perspective(float fovy, float aspect, float n, float f)
    {	float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
        float A = q / aspect;
        float B = (n + f) / (n - f);
        float C = (2.0f * n * f) / (n - f);
        Matrix3D r = new Matrix3D();
        r.setElementAt(0,0,A);
        r.setElementAt(1,1,q);
        r.setElementAt(2,2,B);
        r.setElementAt(3,2,-1.0f);
        r.setElementAt(2,3,C);
        r.setElementAt(3,3,0.0f);
        return r;
    }

    private Matrix3D orthogonal(float left, float right, float top, float bottom, float near, float far)
    {
        Matrix3D r = new Matrix3D();
        r.setElementAt(0,0,2.0/(right-left));
        r.setElementAt(1,1,2.0/(top-bottom));
        r.setElementAt(2,2,1/(far-near));
        r.setElementAt(3,3,1.0f);
        r.setElementAt(0,3,-(right+left)/(right-left));
        r.setElementAt(1,3,-(top+bottom)/(top-bottom));
        r.setElementAt(2, 3, -near/(far-near));
        return r;
    }

    public static void main(String[] args) { new KochSnowflake2D(2.0f, 2); }
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void dispose(GLAutoDrawable drawable) {}

    private int createShaderProgram()
    {	GL4 gl = (GL4) GLContext.getCurrentGL();

        String vshaderSource[] = util.readShaderSource("src/koch_snowflake_2d/vert.shader.2d");
        String fshaderSource[] = util.readShaderSource("src/koch_snowflake_2d/frag.shader.2d");

        int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
        int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

        gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
        gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);

        gl.glCompileShader(vShader);
        gl.glCompileShader(fShader);

        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        gl.glLinkProgram(vfprogram);
        return vfprogram;
    }
}