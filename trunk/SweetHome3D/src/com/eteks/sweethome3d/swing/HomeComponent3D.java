/*
 * HomeComponent3D.java 24 ao?t 2006
 *
 * Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Geometry;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Light;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.CameraEvent;
import com.eteks.sweethome3d.model.CameraListener;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureEvent;
import com.eteks.sweethome3d.model.FurnitureListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.PieceOfFurniture;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.model.WallEvent;
import com.eteks.sweethome3d.model.WallListener;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

/**
 * A component that displays home walls and furniture with Java 3D. 
 * @author Emmanuel Puybaret
 */
public class HomeComponent3D extends JComponent implements Printable {
  private enum ActionType {MOVE_CAMERA_FORWARD, MOVE_CAMERA_FAST_FORWARD, MOVE_CAMERA_BACKWARD, MOVE_CAMERA_FAST_BACKWARD,  
      ROTATE_CAMERA_YAW_LEFT, ROTATE_CAMERA_YAW_FAST_LEFT, ROTATE_CAMERA_YAW_RIGHT, ROTATE_CAMERA_YAW_FAST_RIGHT, 
      ROTATE_CAMERA_PITCH_UP, ROTATE_CAMERA_PITCH_DOWN}
  
  private Home                      home;
  private SimpleUniverse            universe;
  private Map<Object, ObjectBranch> homeObjects = new HashMap<Object, ObjectBranch>();
  private Collection<Object>        homeObjectsToUpdate;
  // Listeners bound to home that updates 3D scene objects
  private CameraListener            cameraListener;
  private PropertyChangeListener    homeCameraListener;
  private PropertyChangeListener    skyColorListener;
  private PropertyChangeListener    groundColorListener;
  private PropertyChangeListener    lightColorListener;
  private WallListener              wallListener;
  private PropertyChangeListener    wallsAlphaListener;
  private FurnitureListener         furnitureListener;

  /**
   * Creates a 3D component that displays <code>home</code> walls and furniture, 
   * with no controller.
   */
  public HomeComponent3D(Home home) {
    this(home, null);  
  }
  
  /**
   * Creates a 3D component that displays <code>home</code> walls and furniture.
   */
  public HomeComponent3D(Home home, HomeController3D controller) {
    this.home = home;
    // Try to get antialiasing
    GraphicsConfigTemplate3D gc = new GraphicsConfigTemplate3D();
    gc.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
    // Create the Java 3D canvas that will display home 
    Canvas3D canvas3D = new Canvas3D(GraphicsEnvironment.getLocalGraphicsEnvironment().
        getDefaultScreenDevice().getBestConfiguration(gc));
    
    // Layout canvas3D
    setLayout(new GridLayout(1, 1));
    add(canvas3D);

    if (controller != null) {
      addMouseListeners(controller, canvas3D);
      createActions(controller);
      installKeyboardActions();
      // Let this component manage focus
      setFocusable(true);
    }

    // Add a hierarchy listener to create canvas universe once this component is made visible 
    // and clean up universe once its parent frame is disposed
    addHierarchyListener(canvas3D, home);
  }

  /**
   * Adds a hierarchy listener to this component to manage canvas universe 
   * creation and clean up.  
   */
  private void addHierarchyListener(final Canvas3D canvas3D, 
                                    final Home home) {
    addAncestorListener(new AncestorListener() {        
        public void ancestorAdded(AncestorEvent event) {
          universe = getUniverse(canvas3D, home);
          canvas3D.setFocusable(false);
        }
        
        public void ancestorRemoved(AncestorEvent event) {
          universe.cleanup();
          removeHomeListeners(home);
        }
        
        public void ancestorMoved(AncestorEvent event) {
        }        
      });
  }

  /**
   * Returns a 3D universe bound to <code>canvas3D</code> 
   * that displays <code>home</code> objects.
   */
  private SimpleUniverse getUniverse(final Canvas3D canvas3D, Home home) {
    // Link canvas 3D to a default universe
    SimpleUniverse universe = new SimpleUniverse(canvas3D);
    
    View view = universe.getViewer().getView();
    // Update field of view from current camera
    updateView(view, home.getCamera(), home.getObserverCamera() == home.getCamera());
    
    TransformGroup viewPlatformTransform = 
        universe.getViewingPlatform().getViewPlatformTransform();
    // Update point of view from current camera
    updateViewPlatformTransform(viewPlatformTransform, home.getCamera());
    
    // Add camera listeners to update later point of view from camera
    addCameraListeners(home, view, viewPlatformTransform);
    
    // Link scene matching home to universe
    universe.addBranchGraph(getSceneTree(home));
    
    return universe;
  }
  
  /**
   * Remove all listeners bound to home that updates 3D scene objects.
   */
  private void removeHomeListeners(Home home) {
    home.removeCameraListener(this.cameraListener);
    home.removePropertyChangeListener(Home.Property.CAMERA, this.homeCameraListener);
    home.removePropertyChangeListener(Home.Property.SKY_COLOR, this.skyColorListener);
    home.removePropertyChangeListener(Home.Property.GROUND_COLOR, this.groundColorListener);
    home.removePropertyChangeListener(Home.Property.LIGHT_COLOR, this.lightColorListener);
    home.removeWallListener(this.wallListener);
    home.removePropertyChangeListener(Home.Property.WALLS_ALPHA, this.wallsAlphaListener);
    home.removeFurnitureListener(this.furnitureListener);
  }

  /**
   * Prints this component to make it fill <code>pageFormat</code> imageable size.
   */
  public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
    if (pageIndex == 0) {
      int printSize = (int)Math.min(pageFormat.getImageableWidth(), 
          pageFormat.getImageableHeight());
      Canvas3D offScreenCanvas = createOffScreenCanvas(printSize);
  
      SimpleUniverse printUniverse = null;
      if (this.universe == null) {
        printUniverse = getUniverse(offScreenCanvas, this.home);
      } else {
        // Create a view associated with canvas3D
        View view = new View();
        view.addCanvas3D(offScreenCanvas);
        // Reuse universe physical body, environment, projection policy, field of view and clip distances
        Viewer universeViewer = this.universe.getViewer();
        view.setPhysicalBody(universeViewer.getPhysicalBody());
        view.setPhysicalEnvironment(universeViewer.getPhysicalEnvironment());
        view.setProjectionPolicy(universeViewer.getView().getProjectionPolicy());
        view.setFieldOfView(universeViewer.getView().getFieldOfView());
        view.setFrontClipDistance(universeViewer.getView().getFrontClipDistance());
        view.setBackClipDistance(universeViewer.getView().getBackClipDistance());
        
        // Create a viewing platform and attach it to view and universe locale
        ViewingPlatform viewingPlatform = new ViewingPlatform();
        viewingPlatform.setUniverse(this.universe);
        this.universe.getLocale().addBranchGraph(
            (BranchGroup)viewingPlatform.getViewPlatformTransform().getParent());
        view.attachViewPlatform(viewingPlatform.getViewPlatform());
        // Reuse universe view platform transform
        Transform3D universeViewPlatformTransform = new Transform3D();
        this.universe.getViewingPlatform().getViewPlatformTransform().getTransform(universeViewPlatformTransform);
        viewingPlatform.getViewPlatformTransform().setTransform(universeViewPlatformTransform);
      }
  
      offScreenCanvas.renderOffScreenBuffer();
      offScreenCanvas.waitForOffScreenRendering();
      BufferedImage image = offScreenCanvas.getOffScreenBuffer().getImage();
  
      Graphics2D g2D = (Graphics2D)g.create();
      // Center the 3D view in component
      g2D.translate(pageFormat.getImageableX() + (pageFormat.getImageableWidth() - printSize) / 2, 
          pageFormat.getImageableY() + (pageFormat.getImageableHeight() - printSize) / 2);
      g2D.drawImage(image, 0, 0, printSize, printSize, this);
      g2D.dispose();
      
      if (printUniverse != null) {
        printUniverse.cleanup();
        removeHomeListeners(this.home);
      } else {
        offScreenCanvas.getView().removeCanvas3D(offScreenCanvas);
      }
      return PAGE_EXISTS;
    } else {
      return NO_SUCH_PAGE;
    }
  }

  /**
   * Returns an off screen canvas.
   */
  private Canvas3D createOffScreenCanvas(int printSize) {
    GraphicsConfigTemplate3D gc = new GraphicsConfigTemplate3D();
    gc.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
    // Create the Java 3D canvas that will display home 
    Canvas3D offScreenCanvas = new Canvas3D(GraphicsEnvironment.getLocalGraphicsEnvironment().
        getDefaultScreenDevice().getBestConfiguration(gc), true);
    Screen3D screen3D = offScreenCanvas.getScreen3D();
    int canvas3DImageSize = printSize * 2;  
    screen3D.setSize(canvas3DImageSize, canvas3DImageSize);
    screen3D.setPhysicalScreenWidth(2f);
    screen3D.setPhysicalScreenHeight(2f);
    BufferedImage image = new BufferedImage(canvas3DImageSize, canvas3DImageSize, BufferedImage.TYPE_INT_RGB);
    ImageComponent2D imageComponent2D = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, image);
    imageComponent2D.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
    offScreenCanvas.setOffScreenBuffer(imageComponent2D);
    return offScreenCanvas;
  }

  /**
   * Adds listeners to home to update point of view from current camera.
   */
  private void addCameraListeners(final Home home, final View view, 
                                  final TransformGroup viewPlatformTransform) {
    this.cameraListener = new CameraListener() {
        public void cameraChanged(CameraEvent ev) {
          // Update view transform later to avoid flickering in case of mulitple camera changes 
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                updateView(view, home.getCamera(), home.getObserverCamera() == home.getCamera());
                updateViewPlatformTransform(viewPlatformTransform, home.getCamera());
              }
            });
        }
      };
    home.addCameraListener(this.cameraListener);
    this.homeCameraListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateView(view, home.getCamera(), home.getObserverCamera() == home.getCamera());
          updateViewPlatformTransform(viewPlatformTransform, home.getCamera());
        }
      };
    home.addPropertyChangeListener(Home.Property.CAMERA, this.homeCameraListener);
  }

  /**
   * Updates <code>view</code> from <code>camera</code> field of view.
   */
  private void updateView(View view, Camera camera, boolean observerCamera) {
    float fieldOfView = camera.getFieldOfView();
    if (fieldOfView == 0) {
      fieldOfView = (float)(Math.PI * 63 / 180);
    }
    view.setFieldOfView(fieldOfView);
    // Use a different front clip distance for observer camera 
    // to obtain better results
    double frontClipDistance = observerCamera ? 2 : 20;
    // Update front and back clip distance to ensure their ratio is less than 3000
    view.setFrontClipDistance(frontClipDistance);
    view.setBackClipDistance(frontClipDistance * 3000);
  }
  
  /**
   * Updates <code>viewPlatformTransform</code> transform from <code>camera</code> angles and location.
   */
  private void updateViewPlatformTransform(TransformGroup viewPlatformTransform, Camera camera) {
    Transform3D yawRotation = new Transform3D();
    yawRotation.rotY(-camera.getYaw() + Math.PI);
    
    Transform3D pitchRotation = new Transform3D();
    pitchRotation.rotX(-camera.getPitch());
    yawRotation.mul(pitchRotation);
    
    Transform3D transform = new Transform3D();
    transform.setTranslation(new Vector3f(camera.getX(), camera.getZ(), camera.getY()));
    transform.mul(yawRotation);
    
    viewPlatformTransform.setTransform(transform);
  }
  
  /**
   * Adds AWT mouse listeners to <code>canvas3D</code> that calls back <code>controller</code> methods.  
   */
  private void addMouseListeners(final HomeController3D controller, Component canvas3D) {
    MouseInputAdapter mouseListener = new MouseInputAdapter() {
        private int xLastMouseMove;
        private int yLastMouseMove;
        
        @Override
        public void mousePressed(MouseEvent ev) {
          if (ev.isPopupTrigger()) {
            mouseReleased(ev);
          } else if (isEnabled()) {
            requestFocusInWindow();
            this.xLastMouseMove = ev.getX();
            this.yLastMouseMove = ev.getY();
          }
        }
  
        @Override
        public void mouseReleased(MouseEvent ev) {
          if (ev.isPopupTrigger()) {
            getComponentPopupMenu().show(HomeComponent3D.this, ev.getX(), ev.getY());
          } 
        }
  
        @Override
        public void mouseDragged(MouseEvent ev) {
          if (isEnabled()) {
            if (ev.isAltDown()) {
              // Mouse move along Y axis while alt is down changes camera location
              float delta = 0.5f * (this.yLastMouseMove - ev.getY());
              // Multiply delta by 10 if shift isn't down
              if (!ev.isShiftDown()) {
                delta *= 10;
              } 
              controller.moveCamera(delta);
            } else {
              final float ANGLE_FACTOR = 0.007f;
              // Mouse move along X axis changes camera yaw 
              float yawDelta = ANGLE_FACTOR * (ev.getX() - this.xLastMouseMove);
              // Multiply yaw delta by 10 if shift isn't down
              if (!ev.isShiftDown()) {
                yawDelta *= 10;
              } 
              controller.rotateCameraYaw(yawDelta);
              
              // Mouse move along Y axis changes camera pitch 
              float pitchDelta = ANGLE_FACTOR * (ev.getY() - this.yLastMouseMove);
              controller.rotateCameraPitch(pitchDelta);
            }
            
            this.xLastMouseMove = ev.getX();
            this.yLastMouseMove = ev.getY();
          }
        }
      };
    MouseWheelListener mouseWheelListener = new MouseWheelListener() {
        public void mouseWheelMoved(MouseWheelEvent ev) {
          if (isEnabled()) {
            // Mouse wheel changes camera location 
            float delta = -ev.getWheelRotation();
            // Multiply delta by 10 if shift isn't down
            if (!ev.isShiftDown()) {
              delta *= 10;
            } 
            controller.moveCamera(delta);
          }
        }
      };
    
    canvas3D.addMouseListener(mouseListener);
    canvas3D.addMouseMotionListener(mouseListener);
    canvas3D.addMouseWheelListener(mouseWheelListener);
    // Add a mouse listener to this component to request focus in case user clicks in component border
    this.addMouseListener(new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          requestFocusInWindow();
        }
      });
  }

  /**
   * Installs keys bound to actions. 
   */
  private void installKeyboardActions() {
    InputMap inputMap = getInputMap(WHEN_FOCUSED);
    inputMap.put(KeyStroke.getKeyStroke("shift UP"), ActionType.MOVE_CAMERA_FORWARD);
    inputMap.put(KeyStroke.getKeyStroke("UP"), ActionType.MOVE_CAMERA_FAST_FORWARD);
    inputMap.put(KeyStroke.getKeyStroke("shift DOWN"), ActionType.MOVE_CAMERA_BACKWARD);
    inputMap.put(KeyStroke.getKeyStroke("DOWN"), ActionType.MOVE_CAMERA_FAST_BACKWARD);
    inputMap.put(KeyStroke.getKeyStroke("shift LEFT"), ActionType.ROTATE_CAMERA_YAW_LEFT);
    inputMap.put(KeyStroke.getKeyStroke("LEFT"), ActionType.ROTATE_CAMERA_YAW_FAST_LEFT);
    inputMap.put(KeyStroke.getKeyStroke("shift RIGHT"), ActionType.ROTATE_CAMERA_YAW_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke("RIGHT"), ActionType.ROTATE_CAMERA_YAW_FAST_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), ActionType.ROTATE_CAMERA_PITCH_UP);
    inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), ActionType.ROTATE_CAMERA_PITCH_DOWN);
  }
 
  /**
   * Creates actions that calls back <code>controller</code> methods.  
   */
  private void createActions(final HomeController3D controller) {
    // Move camera action mapped to arrow keys 
    class MoveCameraAction extends AbstractAction {
      private final int delta;
      
      public MoveCameraAction(int delta) {
        this.delta = delta;
      }

      public void actionPerformed(ActionEvent e) {
        controller.moveCamera(this.delta);
      }
    }
    // Rotate camera yaw action mapped to arrow keys 
    class RotateCameraYawAction extends AbstractAction {
      private final float delta;
      
      public RotateCameraYawAction(float delta) {
        this.delta = delta;
      }

      public void actionPerformed(ActionEvent e) {
        controller.rotateCameraYaw(this.delta);
      }
    }
    // Rotate camera pitch action mapped to arrow keys 
    class RotateCameraPitchAction extends AbstractAction {
      private final float delta;
      
      public RotateCameraPitchAction(float delta) {
        this.delta = delta;
      }

      public void actionPerformed(ActionEvent e) {
        controller.rotateCameraPitch(this.delta);
      }
    }
    ActionMap actionMap = getActionMap();
    actionMap.put(ActionType.MOVE_CAMERA_FORWARD, new MoveCameraAction(1));
    actionMap.put(ActionType.MOVE_CAMERA_FAST_FORWARD, new MoveCameraAction(10));
    actionMap.put(ActionType.MOVE_CAMERA_BACKWARD, new MoveCameraAction(-1));
    actionMap.put(ActionType.MOVE_CAMERA_FAST_BACKWARD, new MoveCameraAction(-10));
    actionMap.put(ActionType.ROTATE_CAMERA_YAW_LEFT, new RotateCameraYawAction(-(float)Math.PI / 180));
    actionMap.put(ActionType.ROTATE_CAMERA_YAW_FAST_LEFT, new RotateCameraYawAction(-(float)Math.PI / 18));
    actionMap.put(ActionType.ROTATE_CAMERA_YAW_RIGHT, new RotateCameraYawAction((float)Math.PI / 180));
    actionMap.put(ActionType.ROTATE_CAMERA_YAW_FAST_RIGHT, new RotateCameraYawAction((float)Math.PI / 18));
    actionMap.put(ActionType.ROTATE_CAMERA_PITCH_UP, new RotateCameraPitchAction(-(float)Math.PI / 180));
    actionMap.put(ActionType.ROTATE_CAMERA_PITCH_DOWN, new RotateCameraPitchAction((float)Math.PI / 180));
  }

  /**
   * Returns scene tree root.
   */
  private BranchGroup getSceneTree(Home home) {
    BranchGroup root = new BranchGroup();

    // Build scene tree
    root.addChild(getHomeTree(home));
    root.addChild(getBackgroundNode(home));
    root.addChild(getGroundNode(home));
    for (Light light : getLights(home)) {
      root.addChild(light);
    }

    return root;
  }

  /**
   * Returns the background node.  
   */
  private Node getBackgroundNode(final Home home) {
    final Background background = new Background();
    updateBackgroundColor(background, home);
    // Allow background color to change
    background.setCapability(Background.ALLOW_COLOR_WRITE);
    background.setApplicationBounds(new BoundingBox(
        new Point3d(-100000, -100000, -100000), 
        new Point3d(100000, 100000, 100000)));
    
    // Add a listener on sky color property change to home
    this.skyColorListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateBackgroundColor(background, home);
        }
      };
    home.addPropertyChangeListener(Home.Property.SKY_COLOR, this.skyColorListener);
    return background;
  }

  /**
   * Updates<code>background</code> color from <code>home</code> sky color.
   */
  private void updateBackgroundColor(Background background, Home home) {
    background.setColor(new Color3f(new Color(home.getSkyColor())));
  }
  
  /**
   * Returns the ground node.  
   */
  private Node getGroundNode(final Home home) {
    // Use coloring attributes for ground to avoid ground lighting
    final ColoringAttributes groundColoringAttributes = new ColoringAttributes();
    groundColoringAttributes.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
    updateGroundColor(groundColoringAttributes, home);
    
    Appearance groundAppearance = new Appearance();
    groundAppearance.setColoringAttributes(groundColoringAttributes);
    
    // Allow ground material to change
    Box groundBox = new Box(1E5f, 0, 1E5f, groundAppearance); 
    Shape3D topShape = groundBox.getShape(Box.TOP);
    groundBox.removeChild(topShape);
    
    // Add a listener on ground color property change to home
    this.groundColorListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateGroundColor(groundColoringAttributes, home);
        }
      };
    home.addPropertyChangeListener(Home.Property.GROUND_COLOR, this.groundColorListener); 
    
    return topShape;
  }
  
  /**
   * Updates ground coloring attributes from <code>home</code> ground color.
   */
  private void updateGroundColor(ColoringAttributes groundColoringAttributes, 
                                 Home home) {
    Color3f groundColor = new Color3f(new Color(home.getGroundColor()));
    groundColoringAttributes.setColor(groundColor);
  }
  
  /**
   * Returns the lights of the scene.
   */
  private Light [] getLights(final Home home) {
    final Light [] lights = {
        new DirectionalLight(new Color3f(), new Vector3f(1.5f, -0.8f, -1)),         
        new DirectionalLight(new Color3f(), new Vector3f(-1.5f, -0.8f, -1)), 
        new DirectionalLight(new Color3f(), new Vector3f(0, -0.8f, 1)), 
        new AmbientLight(new Color3f(0.2f, 0.2f, 0.2f))}; 
    for (int i = 0; i < lights.length - 1; i++) {
      updateLightColor(lights [i], home);
      // Allow directional lights color to change
      lights [i].setCapability(DirectionalLight.ALLOW_COLOR_WRITE);
    }
    
    for (Light light : lights) {
      light.setInfluencingBounds(new BoundingSphere(new Point3d(), 10000));
    }
    
    // Add a listener on light color property change to home
    this.lightColorListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          for (int i = 0; i < lights.length - 1; i++) {
            updateLightColor(lights [i], home);
          }
        }
      };
    home.addPropertyChangeListener(Home.Property.LIGHT_COLOR, this.lightColorListener); 
    
    return lights;
  }

  /**
   * Updates<code>light</code> color from <code>home</code> light color.
   */
  private void updateLightColor(Light light, Home home) {
    light.setColor(new Color3f(new Color(home.getLightColor())));
  }
  
  /**
   * Returns <code>home</code> tree node, with branches for each wall 
   * and piece of furniture of <code>home</code>. 
   */
  private Node getHomeTree(Home home) {
    Group homeRoot = getHomeRoot();
    // Add walls and pieces already available 
    for (Wall wall : home.getWalls()) {
      addWall(homeRoot, wall, home);
    }
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      addPieceOfFurniture(homeRoot, piece);
    }
    // Add wall and furniture listeners to home for further update
    addWallListener(home, homeRoot);
    addFurnitureListener(home, homeRoot);
    return homeRoot;
  }

  /**
   * Returns the group at home subtree root.
   */
  private Group getHomeRoot() {
    Group homeGroup = new Group();    
    //  Allow group to have new children
    homeGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
    homeGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    return homeGroup;
  }

  /**
   * Adds a wall listener and walls alpha change listener to <code>home</code> 
   * that updates the scene <code>homeRoot</code>, each time a wall is added, updated or deleted. 
   */
  private void addWallListener(final Home home, final Group homeRoot) {
    this.wallListener = new WallListener() {
        public void wallChanged(WallEvent ev) {
          Wall wall = ev.getWall();
          switch (ev.getType()) {
            case ADD :
              addWall(homeRoot, wall, home);
              break;
            case UPDATE :
              updateWall(wall);
              break;
            case DELETE :
              deleteObject(wall);
              break;
          }
        }
      };
    home.addWallListener(this.wallListener);
    this.wallsAlphaListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateObjects(home.getWalls());
        }
      };
    home.addPropertyChangeListener(Home.Property.WALLS_ALPHA, this.wallsAlphaListener); 
  }

  /**
   * Adds a furniture listener to <code>home</code> that updates the scene <code>homeRoot</code>, 
   * each time a piece of furniture is added, updated or deleted. 
   */
  private void addFurnitureListener(final Home home, final Group homeRoot) {
    this.furnitureListener = new FurnitureListener() {
        public void pieceOfFurnitureChanged(FurnitureEvent ev) {
          HomePieceOfFurniture piece = (HomePieceOfFurniture)ev.getPieceOfFurniture();
          switch (ev.getType()) {
            case ADD :
              addPieceOfFurniture(homeRoot, piece);
              break;
            case UPDATE :
              updatePieceOfFurniture(piece);
              break;
            case DELETE :
              deleteObject(piece);
              break;
          }
          // If piece is a door or a window, update walls that intersect with piece
          if (piece.isDoorOrWindow()) {
            updateObjects(home.getWalls());
          }
        }
      };
    home.addFurnitureListener(this.furnitureListener);
  }

  /**
   * Adds to <code>homeRoot</code> a wall branch matching <code>wall</code>.
   */
  private void addWall(Group homeRoot, Wall wall, Home home) {
    Wall3D wall3D = new Wall3D(wall, home);
    this.homeObjects.put(wall, wall3D);
    homeRoot.addChild(wall3D);
  }

  /**
   * Updates <code>wall</code> geometry, 
   * and the walls at its end or start.
   */
  private void updateWall(Wall wall) {
    Collection<Wall> wallsToUpdate = new ArrayList<Wall>(3);
    wallsToUpdate.add(wall);
    if (wall.getWallAtStart() != null) {
      wallsToUpdate.add(wall.getWallAtStart());                
    }
    if (wall.getWallAtEnd() != null) {
      wallsToUpdate.add(wall.getWallAtEnd());                
    }
    updateObjects(wallsToUpdate);
  }
  
  /**
   * Detaches from the scene the branch matching <code>homeObject</code>.
   */
  private void deleteObject(Object homeObject) {
    this.homeObjects.get(homeObject).detach();
    this.homeObjects.remove(homeObject);
  }

  /**
   * Adds to <code>homeRoot</code> a piece branch matching <code>piece</code>.
   */
  private void addPieceOfFurniture(Group homeRoot, HomePieceOfFurniture piece) {
    HomePieceOfFurniture3D piece3D = new HomePieceOfFurniture3D(piece);
    this.homeObjects.put(piece, piece3D);
    homeRoot.addChild(piece3D);
  }

  /**
   * Updates <code>piece</code> scale, angle and location.
   */
  private void updatePieceOfFurniture(HomePieceOfFurniture piece) {
    updateObjects(Arrays.asList(new HomePieceOfFurniture [] {piece}));
  }

  /**
   * Updates <code>objects</code> later. Sould be invoked from Event Dispatch Thread.
   */
  private void updateObjects(Collection<? extends Object> objects) {
    if (this.homeObjectsToUpdate != null) {
      this.homeObjectsToUpdate.addAll(objects);
    } else {
      this.homeObjectsToUpdate = new HashSet<Object>(objects);
      // Invoke later the update of objects of homeObjectsToUpdate
      EventQueue.invokeLater(new Runnable () {
        public void run() {
          for (Object object : homeObjectsToUpdate) {
            ObjectBranch objectBranch = homeObjects.get(object);
            // Check object wasn't deleted since updateObjects call
            if (objectBranch != null) { 
              homeObjects.get(object).update();
            }
          }
          homeObjectsToUpdate = null;
        }
      });
    }
  }
  
  /**
   * Root of a branch that matches a home object. 
   */
  private static abstract class ObjectBranch extends BranchGroup {
    public abstract void update();
  }

  /**
   * Root of wall branch.
   */
  private static class Wall3D extends ObjectBranch {
    private static final Material DEFAULT_MATERIAL = new Material();
    
    private static final int LEFT_WALL_SIDE  = 0;
    private static final int RIGHT_WALL_SIDE = 1;
    
    private Home home;

    public Wall3D(Wall wall, Home home) {
      setUserData(wall);
      this.home = home;

      // Allow wall branch to be removed from its parent
      setCapability(BranchGroup.ALLOW_DETACH);
      // Allow to read branch shape children
      setCapability(BranchGroup.ALLOW_CHILDREN_READ);
      
      // Add wall left and right empty shapes to branch
      addChild(getWallPartShape());
      addChild(getWallPartShape());
      // Set wall shape geometry and appearance
      updateWallGeometry();
      updateWallAppearance();
    }

    /**
     * Returns a wall part shape with no geometry  
     * and a default appearance with a white material.
     */
    private Node getWallPartShape() {
      Shape3D wallShape = new Shape3D();
      // Allow wall shape to change its geometry
      wallShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
      wallShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      wallShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

      Appearance wallAppearance = new Appearance();
      wallAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
      wallAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
      wallAppearance.setMaterial(DEFAULT_MATERIAL);
      TransparencyAttributes transparencyAttributes = new TransparencyAttributes();
      transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
      transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
      wallAppearance.setTransparencyAttributes(transparencyAttributes);
      wallShape.setAppearance(wallAppearance);
      
      return wallShape;
    }

    @Override
    public void update() {
      updateWallGeometry();
      updateWallAppearance();
    }
    
    /**
     * Sets the 3D geometry of this wall shapes that matches its 2D geometry.  
     */
    private void updateWallGeometry() {
      updateWallSideGeometry(LEFT_WALL_SIDE);
      updateWallSideGeometry(RIGHT_WALL_SIDE);
    }
    
    private void updateWallSideGeometry(int wallSide) {
      Shape3D wallShape = (Shape3D)getChild(wallSide);
      int currentGeometriesCount = wallShape.numGeometries();
      for (Geometry wallGeometry : getWallGeometries(wallSide)) {
        wallShape.addGeometry(wallGeometry);
      }
      for (int i = currentGeometriesCount - 1; i >= 0; i--) {
        wallShape.removeGeometry(i);
      }
    }
    
    /**
     * Returns <code>wall</code> geometries computed with windows or doors 
     * that intersect wall.
     */
    private Geometry[] getWallGeometries(int wallSide) {
      Shape wallShape = getShape(getWallSidePoints(wallSide));
      float wallHeight = getWallHeight();
      // Search which doors or windows intersect with this wall side
      Map<HomePieceOfFurniture, Area> intersections = new HashMap<HomePieceOfFurniture, Area>();
      for (HomePieceOfFurniture piece : this.home.getFurniture()) {
        if (piece.isDoorOrWindow() 
            && piece.getElevation() < wallHeight) {
          Shape pieceShape = getShape(piece.getPoints());
          Area wallArea = new Area(wallShape);
          wallArea.intersect(new Area(pieceShape));
          boolean wallPieceIntersectionEmpty = wallArea.isEmpty();
          if (!wallPieceIntersectionEmpty) {
            intersections.put(piece, wallArea);
          }
        }
      }
      List<Geometry> wallGeometries = new ArrayList<Geometry>();
      List<float[]> wallPoints = new ArrayList<float[]>(4);
      // Get wall shape excluding window intersections
      Area wallArea = new Area(wallShape);
      for (Area intersection : intersections.values()) {
        wallArea.exclusiveOr(intersection);
      }
      
      // Generate geometry for each wall part that doesn't contain a window
      for (PathIterator it = wallArea.getPathIterator(null); !it.isDone(); ) {
        float [] wallPoint = new float[2];
        if (it.currentSegment(wallPoint) == PathIterator.SEG_CLOSE) {
          float [][] wallPartPoints = wallPoints.toArray(new float[wallPoints.size()][]);
          // Compute geometry for vertical part
          wallGeometries.add(getWallVerticalPartGeometry(wallPartPoints, 0, wallHeight));
          // Compute geometry for bottom part
          wallGeometries.add(getWallHorizontalPartGeometry(wallPartPoints, 0));
          // Compute geometry for top part
          wallGeometries.add(getWallHorizontalPartGeometry(wallPartPoints, wallHeight));
          wallPoints.clear();
        } else {
          wallPoints.add(wallPoint);
        }
        it.next();
      }
      
      // Generate geometry for each wall part above and below a window
      for (Entry<HomePieceOfFurniture, Area> windowIntersection : intersections.entrySet()) {
        for (PathIterator it = windowIntersection.getValue().getPathIterator(null); !it.isDone(); ) {
          float [] wallPoint = new float[2];
          if (it.currentSegment(wallPoint) == PathIterator.SEG_CLOSE) {
            float [][] wallPartPoints = wallPoints.toArray(new float[wallPoints.size()][]);
            HomePieceOfFurniture doorOrWindow = windowIntersection.getKey();            
            float doorOrWindowTop = doorOrWindow.getElevation() + doorOrWindow.getHeight();
            // Generate geometry for wall part above window
            if (doorOrWindowTop < wallHeight) {
              wallGeometries.add(getWallVerticalPartGeometry(
                  wallPartPoints, doorOrWindowTop, wallHeight));
              wallGeometries.add(getWallHorizontalPartGeometry(
                  wallPartPoints, doorOrWindowTop));
              wallGeometries.add(getWallHorizontalPartGeometry(wallPartPoints, wallHeight));
            }
            // Generate geometry for wall part below window
            if (doorOrWindow.getElevation() > 0) {
              wallGeometries.add(getWallVerticalPartGeometry(
                  wallPartPoints, 0, doorOrWindow.getElevation()));
              wallGeometries.add(getWallHorizontalPartGeometry(wallPartPoints, 0));
              wallGeometries.add(getWallHorizontalPartGeometry(wallPartPoints, doorOrWindow.getElevation()));
            }
            wallPoints.clear();
          } else {
            wallPoints.add(wallPoint);
          }
          it.next();
        }
      }
      return wallGeometries.toArray(new Geometry [wallGeometries.size()]);
    }
    
    /**
     * Returns the shape matching the coordinates in <code>points</code> array.
     */
    private Shape getShape(float [][] points) {
      GeneralPath wallPath = new GeneralPath();
      wallPath.moveTo(points [0][0], points [0][1]);
      for (int i = 1; i < points.length; i++) {
        wallPath.lineTo(points [i][0], points [i][1]);
      }
      wallPath.closePath();
      return wallPath;
    }
    
    /**
     * Returns the points of one of the side of this wall. 
     */
    private float [][] getWallSidePoints(int wallSide) {
      float [][] wallPoints = ((Wall)getUserData()).getPoints();
      // Compute coordinates of the point at middle of wallPoints[0] and wallPoints[3]
      float xP0P3Middle = (wallPoints[0][0] + wallPoints[3][0]) / 2;
      float yP0P3Middle = (wallPoints[0][1] + wallPoints[3][1]) / 2;
      // Compute coordinates of the point at middle of wallPoints[1] and wallPoints[2]
      float xP1P2Middle = (wallPoints[1][0] + wallPoints[2][0]) / 2;
      float yP1P2Middle = (wallPoints[1][1] + wallPoints[2][1]) / 2;
      
      if (wallSide == LEFT_WALL_SIDE) {
        wallPoints [2][0] = xP1P2Middle;
        wallPoints [2][1] = yP1P2Middle;
        wallPoints [3][0] = xP0P3Middle;
        wallPoints [3][1] = yP0P3Middle;
      } else { // RIGHT_WALL_SIDE
        wallPoints [1][0] = xP1P2Middle;
        wallPoints [1][1] = yP1P2Middle;
        wallPoints [0][0] = xP0P3Middle;
        wallPoints [0][1] = yP0P3Middle;
      }
      return wallPoints;
    }

    /**
     * Returns the vertical rectangles that join each point of <code>points</code>
     * and spread from <code>yMin</code> to <code>yMax</code>.
     */
    private Geometry getWallVerticalPartGeometry(float [][] points, float yMin, float yMax) {
      Point3f [] bottom = new Point3f [points.length];
      Point3f [] top    = new Point3f [points.length];
      for (int i = 0; i < points.length; i++) {
        bottom [i] = new Point3f(points[i][0], yMin, points[i][1]);
        top [i]    = new Point3f(points[i][0], yMax, points[i][1]);
      }
      Point3f [] coords = new Point3f [points.length * 4];
      int j = 0;
      for (int i = 0; i < points.length - 1; i++) {
        coords [j++] = bottom [i];
        coords [j++] = bottom [i + 1];
        coords [j++] = top [i + 1];
        coords [j++] = top [i];
      }
      coords [j++] = bottom [points.length - 1];
      coords [j++] = bottom [0];
      coords [j++] = top [0];
      coords [j++] = top [points.length - 1];
      
      GeometryInfo geometryInfo = new GeometryInfo (GeometryInfo.QUAD_ARRAY);
      geometryInfo.setCoordinates (coords);
      // Generate normals
      new NormalGenerator(0).generateNormals(geometryInfo);
      return geometryInfo.getIndexedGeometryArray();
    }

    /**
     * Returns the geometry of the top or bottom part of a wall at <code>y</code>.
     */
    private Geometry getWallHorizontalPartGeometry(float [][] points, float y) {
      Point3f [] coords = new Point3f [points.length];
      for (int i = 0; i < points.length; i++) {
        coords [i] = new Point3f(points[i][0], y, points[i][1]);
      }
      GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
      geometryInfo.setCoordinates (coords);
      geometryInfo.setStripCounts(new int [] {coords.length});
      // Generate normals
      new NormalGenerator(0).generateNormals(geometryInfo);
      return geometryInfo.getIndexedGeometryArray ();
    }
    
    /**
     * Returns the height of tha wall managed by this 3D object.
     */
    private float getWallHeight() {
      Float wallHeight = ((Wall)getUserData()).getHeight();      
      if (wallHeight != null) {
        return wallHeight;
      } else {
        // If wall height isn't set, use home wall height
        return this.home.getWallHeight();
      }
    }
    
    /**
     * Sets wall appearance with its color.
     */
    private void updateWallAppearance() {
      Wall wall = (Wall)getUserData();
      // Update material of wall left part
      Integer leftSideColor = wall.getLeftSideColor();
      Appearance wallAppearance = ((Shape3D)getChild(LEFT_WALL_SIDE)).getAppearance();
      if (leftSideColor == null && wallAppearance.getUserData() != null
          || leftSideColor != null && !leftSideColor.equals(wallAppearance.getUserData())) {
        // Store color in appearance user data to avoid appearance update at each wall update 
        wallAppearance.setUserData(leftSideColor);
        wallAppearance.setMaterial(getMaterial(leftSideColor));
      }
      // Update wall transparency
      float wallsAlpha = this.home.getWallsAlpha();
      TransparencyAttributes transparencyAttributes = wallAppearance.getTransparencyAttributes();
      transparencyAttributes.setTransparency(wallsAlpha);
      // If walls alpha is equal to zero, turn off transparency to get better results 
      transparencyAttributes.setTransparencyMode(wallsAlpha == 0 
          ? TransparencyAttributes.NONE 
          : TransparencyAttributes.NICEST);

      // Update material of wall right part
      Integer rightSideColor = wall.getRightSideColor();
      wallAppearance = ((Shape3D)getChild(RIGHT_WALL_SIDE)).getAppearance();
      if (rightSideColor == null && wallAppearance.getUserData() != null
          || rightSideColor != null && !rightSideColor.equals(wallAppearance.getUserData())) {
        // Store color in appearance user data to avoid appearance update at each wall update 
        wallAppearance.setUserData(rightSideColor);
        wallAppearance.setMaterial(getMaterial(rightSideColor));
      }
      // Update wall transparency
      transparencyAttributes = wallAppearance.getTransparencyAttributes();
      transparencyAttributes.setTransparency(this.home.getWallsAlpha());
      transparencyAttributes.setTransparencyMode(
          wallsAlpha == 0 ? TransparencyAttributes.NONE : TransparencyAttributes.NICEST);
    }
    
    private Material getMaterial(Integer color) {
      if (color != null) {
        Color3f materialColor = new Color3f(((color >>> 16) & 0xFF) / 256f,
                                            ((color >>> 8) & 0xFF) / 256f,
                                                    (color & 0xFF) / 256f);
        return new Material(materialColor, new Color3f(), materialColor, materialColor, 64);
      } else {
        return DEFAULT_MATERIAL;
      }
    }
  }

  /**
   * Root of piece of furniture branch.
   */
  private static class HomePieceOfFurniture3D extends ObjectBranch {
    private static Executor modelLoader = Executors.newSingleThreadExecutor();

    public HomePieceOfFurniture3D(HomePieceOfFurniture piece) {
      setUserData(piece);      

      // Allow piece branch to be removed from its parent
      setCapability(BranchGroup.ALLOW_DETACH);
      // Allow to read branch transform child
      setCapability(BranchGroup.ALLOW_CHILDREN_READ);
      
      createPieceOfFurnitureNode();

      // Set piece model initial location, orientation and size
      updatePieceOfFurnitureTransform();
    }

    /**
     * Creates the piece node with its transform group and add it to the piece branch. 
     */
    private void createPieceOfFurnitureNode() {
      final TransformGroup pieceTransformGroup = new TransformGroup();
      // Allow the change of the transformation that sets piece size and position
      pieceTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

      pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
      pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
      pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_READ);

      // While loading model use a temporary node that displays a white box  
      final BranchGroup waitBranch = new BranchGroup();
      waitBranch.setCapability(BranchGroup.ALLOW_DETACH);
      waitBranch.addChild(getModelBox(Color.WHITE));      
      // Allow appearance change on all children
      setAppearanceChangeCapability(waitBranch);
      
      pieceTransformGroup.addChild(waitBranch);
      addChild(pieceTransformGroup);
      
      // Load piece real 3D model
      modelLoader.execute(new Runnable() {
          public void run() {
            BranchGroup modelBranch = new BranchGroup();
            modelBranch.addChild(getModelNode());
            // Allow appearance change on all children
            setAppearanceChangeCapability(modelBranch);
            // Add model branch to live scene
            pieceTransformGroup.addChild(modelBranch);
            // Remove temporary node
            waitBranch.detach();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  // Update piece color, visibility and model mirror in dispatch thread as
                  // these attributes may be changed in that thread
                  updatePieceOfFurnitureColor();      
                  updatePieceOfFurnitureVisibility();
                  updatePieceOfFurnitureModelMirrored();
                }
              });
          }
        });
    }

    @Override
    public void update() {
      updatePieceOfFurnitureTransform();
      updatePieceOfFurnitureColor();      
      updatePieceOfFurnitureVisibility();      
      updatePieceOfFurnitureModelMirrored();
    }

    /**
     * Sets the transformation applied to piece model to match
     * its location, its angle and its size.
     */
    private void updatePieceOfFurnitureTransform() {
      HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
      // Set piece size
      Transform3D scale = new Transform3D();
      float pieceWidth = piece.getWidth();
      // If piece model is mirrored, inverse its width
      if (piece.isModelMirrored()) {
        pieceWidth *= -1;
      }
      scale.setScale(new Vector3d(pieceWidth, piece.getHeight(), piece.getDepth()));
      // Change its angle around y axis
      Transform3D orientation = new Transform3D();
      orientation.rotY(-piece.getAngle());
      orientation.mul(scale);
      // Translate it to its location
      Transform3D pieceTransform = new Transform3D();
      pieceTransform.setTranslation(new Vector3f(
          piece.getX(), piece.getElevation() + piece.getHeight() / 2, piece.getY()));      
      pieceTransform.mul(orientation);
      
      // Change model transformation      
      ((TransformGroup)getChild(0)).setTransform(pieceTransform);
    }

    /**
     * Sets the color applied to piece model.
     */
    private void updatePieceOfFurnitureColor() {
      HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
      if (piece.getColor() != null) {
        Integer color = piece.getColor();
        Color3f materialColor = new Color3f(((color >>> 16) & 0xFF) / 256f,
                                             ((color >>> 8) & 0xFF) / 256f,
                                                     (color & 0xFF) / 256f);
        setMaterial(getChild(0), 
            new Material(materialColor, new Color3f(), materialColor, materialColor, 64));
      } else {
        // Set default material of model
        setMaterial(getChild(0), null);
      }
    }

    /**
     * Sets whether this piece model is visible or not.
     */
    private void updatePieceOfFurnitureVisibility() {
      HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
      setVisible(getChild(0), piece.isVisible());
    }

    /**
     * Sets whether this piece model is mirrored or not.
     */
    private void updatePieceOfFurnitureModelMirrored() {
      HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
      // Cull front or back model faces whether its model is mirrored or not
      setCullFace(getChild(0), 
          piece.isModelMirrored() ^ piece.isBackFaceShown() 
              ? PolygonAttributes.CULL_FRONT 
              : PolygonAttributes.CULL_BACK);
    }

    /**
     * Returns the 3D model of this piece that fits 
     * in a 1 unit wide box centered at the origin. 
     */
    private Node getModelNode() {
      PieceOfFurniture piece = (PieceOfFurniture)getUserData();
      // If same model was already loaded return a clone from its cache 
      Content model = piece.getModel();
      
      try {
        BranchGroup modelNode = ModelManager.getInstance().getModel(model);
        // Get model bounding box size
        BoundingBox modelBounds = ModelManager.getInstance().getBounds(modelNode);
        Point3d lower = new Point3d();
        modelBounds.getLower(lower);
        Point3d upper = new Point3d();
        modelBounds.getUpper(upper);
        
        // Translate model to its center
        Transform3D translation = new Transform3D();
        translation.setTranslation(
            new Vector3d(-lower.x - (upper.x - lower.x) / 2, 
                -lower.y - (upper.y - lower.y) / 2, 
                -lower.z - (upper.z - lower.z) / 2));      
        // Scale model to make it fill a 1 unit wide box
        Transform3D scaleOneTransform = new Transform3D();
        scaleOneTransform.setScale (
            new Vector3d(1 / (upper.x -lower.x), 
                1 / (upper.y - lower.y), 
                1 / (upper.z - lower.z)));
        scaleOneTransform.mul(translation);
        // Apply model rotation
        Transform3D modelTransform = new Transform3D();
        float [][] modelRotation = piece.getModelRotation();
        Matrix3f modelRotationMatrix = new Matrix3f(modelRotation [0][0], modelRotation [0][1], modelRotation [0][2],
            modelRotation [1][0], modelRotation [1][1], modelRotation [1][2],
            modelRotation [2][0], modelRotation [2][1], modelRotation [2][2]);
        modelTransform.setRotation(modelRotationMatrix);
        modelTransform.mul(scaleOneTransform);
        
        // Add model scene to transform group
        TransformGroup modelTransformGroup = new TransformGroup(modelTransform);
        modelTransformGroup.addChild(modelNode);
        return modelTransformGroup;
      } catch (IOException ex) {
        // In case of problem return a default box
        return getModelBox(Color.RED);
      } 
    }

    /**
     * Returns a box that may replace model. 
     */
    private Node getModelBox(Color color) {
      Material material = new Material();
      material.setDiffuseColor(new Color3f(color));
      material.setAmbientColor(new Color3f(color.darker()));
      
      Appearance boxAppearance = new Appearance();
      boxAppearance.setMaterial(material);
      return new Box(0.5f, 0.5f, 0.5f, boxAppearance);
    }

    /**
     * Sets the capability to change material and rendering attributes
     * for all children of <code>node</code>.
     */
    private void setAppearanceChangeCapability(Node node) {
      if (node instanceof Group) {
        node.setCapability(Group.ALLOW_CHILDREN_READ);
        Enumeration enumeration = ((Group)node).getAllChildren(); 
        while (enumeration.hasMoreElements()) {
          setAppearanceChangeCapability((Node)enumeration.nextElement());
        }
      } else if (node instanceof Shape3D) {        
        Appearance appearance = ((Shape3D)node).getAppearance();
        if (appearance != null) {
          setAppearanceCapabilities(appearance);
        }
        node.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        node.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
      }
    }

    /**
     * Sets the material attribute of all <code>Shape3D</code> children nodes of <code>node</code> 
     * with a given <code>material</code>. 
     */
    private void setMaterial(Node node, Material material) {
      if (node instanceof Group) {
        // Set material of all children
        Enumeration enumeration = ((Group)node).getAllChildren(); 
        while (enumeration.hasMoreElements()) {
          setMaterial((Node)enumeration.nextElement(), material);
        }
      } else if (node instanceof Shape3D) {
        Shape3D shape = (Shape3D)node;
        String shapeName = (String)shape.getUserData();
        // Change material of all shape that are not window panes
        if (shapeName == null
            || !shapeName.startsWith(ModelManager.WINDOW_PANE_SHAPE_PREFIX)) {
          Appearance appearance = shape.getAppearance();
          if (appearance == null) {
            shape.setAppearance(createAppearanceWithChangeCapabilities());
          }
          // Use appearance user data to store shape default material
          Material defaultMaterial = (Material)appearance.getUserData();
          if (defaultMaterial == null) {
            defaultMaterial = appearance.getMaterial();
            appearance.setUserData(defaultMaterial);
          }
          // Change material
          if (material != null) {
            appearance.setMaterial(material);
          } else {
            // Restore default material
            appearance.setMaterial(defaultMaterial);
          }
        }
      }
    }

    /**
     * Sets the visible attribute of all <code>Shape3D</code> children nodes of <code>node</code>. 
     */
    private void setVisible(Node node, boolean visible) {
      if (node instanceof Group) {
        // Set visibility of all children
        Enumeration enumeration = ((Group)node).getAllChildren(); 
        while (enumeration.hasMoreElements()) {
          setVisible((Node)enumeration.nextElement(), visible);
        }
      } else if (node instanceof Shape3D) {
        Appearance appearance = ((Shape3D)node).getAppearance();
        if (appearance == null) {
          ((Shape3D)node).setAppearance(createAppearanceWithChangeCapabilities());
        }
        RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
        if (renderingAttributes == null) {
          renderingAttributes = new RenderingAttributes();
          renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
          appearance.setRenderingAttributes(renderingAttributes);
        }
        
        // Change visibility
        renderingAttributes.setVisible(visible);
      }
    }

    /**
     * Sets the cull face of all <code>Shape3D</code> children nodes of <code>node</code>.
     * @param cullFace <code>PolygonAttributes.CULL_FRONT</code> or <code>PolygonAttributes.CULL_BACK</code>
     */
    private void setCullFace(Node node, int cullFace) {
      if (node instanceof Group) {
        // Set cull face of all children
        Enumeration enumeration = ((Group)node).getAllChildren(); 
        while (enumeration.hasMoreElements()) {
          setCullFace((Node)enumeration.nextElement(), cullFace);
        }
      } else if (node instanceof Shape3D) {
        Appearance appearance = ((Shape3D)node).getAppearance();
        if (appearance == null) {
          ((Shape3D)node).setAppearance(createAppearanceWithChangeCapabilities());
        }
        PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
        if (polygonAttributes == null) {
          polygonAttributes = new PolygonAttributes();
          polygonAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
          appearance.setPolygonAttributes(polygonAttributes);
        }
        
        // Change cull face
        polygonAttributes.setCullFace(cullFace);
      }
    }

    private Appearance createAppearanceWithChangeCapabilities() {
      Appearance appearance = new Appearance();
      setAppearanceCapabilities(appearance);
      return appearance;
    }

    private void setAppearanceCapabilities(Appearance appearance) {
      // Allow future material and rendering attributes changes
      appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
      appearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
      appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
      appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
      appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
      appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
    }
  }
}
