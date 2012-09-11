/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package simulator.framework;

import java.util.TreeMap;

/**
 * Collection of controllers indexed by name.
 * @author justinr2
 */
public final class ControllerSet {
    TreeMap<String, Controller> controllers = new TreeMap<String, Controller>();

    /**
     * All controllers must have a unique name (what is returned by Controller.getName()).
     * 
     * @param controller  the contoller object to add to the set
     * @throws IllegalArgumentException if a controller with the same name is already in the set.
     */
    public void add(Controller controller) {
        String name = controller.getName();
        if (name == null) {
            throw new IllegalArgumentException("getName cannot return null");
        }
        if (controller == null) {
            throw new IllegalArgumentException("Controller argument cannot be null");
        }
        if (controllers.containsKey(name)) {
            throw new IllegalArgumentException("A controller with the name " + name + " already exists in the set.");
        }
        if (controllers.containsValue(controller)) {
            throw new IllegalArgumentException("The specific controller object has already been added to this controller set and cannot be added twice.");
        }
        controllers.put(name, controller);
    }

    /**
     * 
     * @param name  The name (as returned by Controller.getName()).
     * @return the controller, if it exists.
     * @throws IllegalArgumentException if a controller with the given name is not found.
     * 
     */
    public Controller get(String name) {
        if (!controllers.containsKey(name)) {
            throw new IllegalArgumentException("No controller named " + name + " is present in the ControllerSet.");
        }
        return controllers.get(name);
    }

}
