package org.wso2.carbon.lcm.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.wso2.carbon.lcm.core.beans.CustomCodeBean;
import org.wso2.carbon.lcm.core.exception.LifecycleException;
import org.wso2.carbon.lcm.core.impl.LifecycleState;
import org.wso2.carbon.lcm.core.util.LifecycleOperationUtil;
import org.wso2.carbon.lcm.core.util.LifecycleUtils;
import org.wso2.carbon.lcm.sql.beans.LifecycleStateBean;

import java.util.List;

import static org.wso2.carbon.lcm.core.util.LifecycleOperationUtil.changeCheckListItem;
import static org.wso2.carbon.lcm.core.util.LifecycleOperationUtil.getInitialState;
import static org.wso2.carbon.lcm.core.util.LifecycleOperationUtil.populateItems;
import static org.wso2.carbon.lcm.core.util.LifecycleOperationUtil.removeLifecycleStateData;

/**
 * This is the class provides all the logic related to lifecycle operations. (Associate, Dissociate, State change
 * event and Check list item event)
 */
public class LifecycleOperationManager {

    private static Logger log = LoggerFactory.getLogger(LifecycleOperationManager.class);

    /**
     * This method need to call for each and event life cycle state changes.
     *
     * @param targetState                       {@code String} Required target state.
     * @param uuid                              {@code String} Lifecycle id that maps with the asset.
     * @param resource                          {@code Object} The current object to which lifecycle is attached to.
     * @param user                              The user who invoked the action. This will be used for auditing
     *                                          purposes.
     * @return                                  {@code LifecycleState} object of updated life cycle state.
     * @throws LifecycleException               If exception occurred while execute life cycle state change.
     */
    public static LifecycleState executeLifecycleEvent(String targetState, String uuid, String user, Object resource)
            throws LifecycleException {
        LifecycleState nextState = new LifecycleState();
        LifecycleState currentState = LifecycleOperationUtil.getCurrentLifecycleState(uuid);
        if (!validateTargetState(currentState, targetState)) {
            throw new LifecycleException("The specified target state " + targetState + " is not a valid target state. "
                    + "Can't transit from " + currentState + "to" + targetState);
        }
        if (!validateCheckListItemSelected(currentState, targetState)) {
            throw new LifecycleException(
                    "Required checklist items are not selected to perform the state transition operation from "
                            + currentState.getState() + " to " + targetState);
        }
        String lcName = currentState.getLcName();
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        runCustomExecutorsCode(resource, currentState.getCustomCodeBeanList(), currentState.getState(), targetState);
        populateItems(nextState, lcContent);
        nextState.setState(targetState);
        nextState.setLcName(currentState.getLcName());
        nextState.setLifecycleId(currentState.getLifecycleId());
        LifecycleOperationUtil.changeLifecycleState(currentState.getState(), targetState, uuid, user);

        if (log.isDebugEnabled()) {
            log.debug("Lifecycle state was changed from " + currentState.getState() + " to " + targetState
                    + " for lifecycle id " + uuid);
        }
        return nextState;
    }

    /**
     * This method need to call for each and event life cycle state changes.
     *
     * @param currentState {@code String} Current state
     * @param targetState {@code String} Required target state.
     * @param uuid        {@code String} Lifecycle id that maps with the asset.
     * @param resource    {@code Object} The current object to which lifecycle is attached to.
     * @param user        The user who invoked the action. This will be used for auditing
     *                    purposes.
     * @return {@code LifecycleState} object of updated life cycle state.
     * @throws LifecycleException If exception occurred while execute life cycle state change.
     */
    public static LifecycleState executeLifecycleEvent(String currentState, String targetState, String uuid, String
            user, Object resource)
            throws LifecycleException {
        LifecycleState nextState = new LifecycleState();
        LifecycleState currentLifecycleState = getLifecycleDataForState(uuid, currentState);
        if (!validateTargetState(currentLifecycleState, targetState)) {
            throw new LifecycleException("The specified target state " + targetState + " is not a valid target state. "
                    + "Can't transit from " + currentState + " to " + targetState);
        }
        if (!validateCheckListItemSelected(currentLifecycleState, targetState)) {
            throw new LifecycleException(
                    "Required checklist items are not selected to perform the state transition operation from "
                            + currentLifecycleState.getState() + " to " + targetState);
        }
        String lcName = currentLifecycleState.getLcName();
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        runCustomExecutorsCode(resource, currentLifecycleState.getCustomCodeBeanList(), currentLifecycleState
                .getState(), targetState);
        populateItems(nextState, lcContent);
        nextState.setState(targetState);
        nextState.setLcName(currentLifecycleState.getLcName());
        nextState.setLifecycleId(currentLifecycleState.getLifecycleId());
        LifecycleOperationUtil.changeLifecycleState(currentLifecycleState.getState(), targetState, uuid, user);

        if (log.isDebugEnabled()) {
            log.debug("Lifecycle state was changed from " + currentLifecycleState.getState() + " to " + targetState
                    + " for lifecycle id " + uuid);
        }
        return nextState;
    }

    /**
     * This method need to call for each check list item operation.
     *
     * @param uuid                              Object that can use to uniquely identify resource.
     * @param currentState                      The state which the checklist item is associated with.
     * @param checkListItemName                 Name of the check list item as specified in the lc config.
     * @param value                             Value of the check list item. Either selected or not.
     * @return updated LifecycleState
     * @throws LifecycleException               If exception occurred while execute life cycle update.
     */
    public static LifecycleState checkListItemEvent(String uuid, String currentState, String checkListItemName,
            boolean value) throws LifecycleException {
        changeCheckListItem(uuid, currentState, checkListItemName, value);
        LifecycleState currentStateObject = LifecycleOperationUtil.getCurrentLifecycleState(uuid);

        if (log.isDebugEnabled()) {
            log.debug("Check list item " + checkListItemName + " is set to " + value);
        }
        return currentStateObject;
    }


    /**
     * This method is used to associate a lifecycle with an asset.
     *
     * @param lcName                        LC name which associates with the resource.
     * @param user                          The user who invoked the action. This will be used for auditing purposes.
     * @return                              Object of added life cycle state.
     * @throws LifecycleException  If failed to associate life cycle with asset.
     */
    public static LifecycleState addLifecycle(String lcName, String user) throws LifecycleException {
        LifecycleState lifecycleState;
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        lifecycleState = new LifecycleState();

        String initialState = getInitialState(lcContent, lcName);
        lifecycleState.setLcName(lcName);
        lifecycleState.setState(initialState);
        populateItems(lifecycleState, lcContent);
        String lifecycleId = LifecycleOperationUtil.associateLifecycle(lcName, initialState, user);

        lifecycleState.setLifecycleId(lifecycleId);
        if (log.isDebugEnabled()) {
            log.debug("Id : " + lifecycleId + " associated with lifecycle " + lcName + " and initial state set to "
                    + initialState);
        }
        return lifecycleState;
    }

    /**
     * This method is used to associate a lifecycle with an asset. Lifecycle Id can be specified in the request
     * @param lcName                        LC name which associates with the resource.
     * @param lifecycleId                   Unique lifecycle ID
     * @param user                          The user who invoked the action. This will be used for auditing purposes.
     * @return                              Object of added life cycle state.
     * @throws LifecycleException  If failed to associate life cycle with asset.
     */
    public static LifecycleState addLifecycle(String lcName, String lifecycleId, String user) throws
            LifecycleException {
        LifecycleState lifecycleState;
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        lifecycleState = new LifecycleState();

        String initialState = getInitialState(lcContent, lcName);
        lifecycleState.setLcName(lcName);
        lifecycleState.setState(initialState);
        populateItems(lifecycleState, lcContent);
        LifecycleOperationUtil.associateLifecycle(lcName, lifecycleId, initialState, user);

        lifecycleState.setLifecycleId(lifecycleId);
        if (log.isDebugEnabled()) {
            log.debug("Id : " + lifecycleId + " associated with lifecycle " + lcName + " and initial state set to "
                    + initialState);
        }
        return lifecycleState;
    }

    /**
     * This method is used to detach a lifecycle from an asset.
     *
     * @param uuid                      Lifecycle id that maps with the asset.
     * @throws LifecycleException       If failed to associate life cycle with asset.
     */
    public static void removeLifecycle(String uuid) throws LifecycleException {
        removeLifecycleStateData(uuid);
    }

    /**
     * Get current life cycle state object.
     * @param uuid uuid of the LifecycleState
     * @return {@code LifecycleState} object represent current life cycle.
     * @throws LifecycleException
     */
    public static LifecycleState getCurrentLifecycleState(String uuid) throws LifecycleException {
        LifecycleState currentLifecycleState = new LifecycleState();
        LifecycleStateBean lifecycleStateBean = LifecycleOperationUtil.getLCStateDataFromID(uuid);
        String lcName = lifecycleStateBean.getLcName();
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        currentLifecycleState.setLcName(lcName);
        currentLifecycleState.setLifecycleId(uuid);
        currentLifecycleState.setState(lifecycleStateBean.getPostStatus());
        populateItems(currentLifecycleState, lcContent);
        LifecycleOperationUtil.setCheckListItemData(currentLifecycleState, lifecycleStateBean.getCheckListData());
        return currentLifecycleState;
    }

    public static LifecycleState getLifecycleDataForState (String uuid, String lcState) throws LifecycleException {
        LifecycleState currentLifecycleState = new LifecycleState();
        LifecycleStateBean lifecycleStateBean = LifecycleOperationUtil.getLCDataFromState(uuid, lcState);
        String lcName = lifecycleStateBean.getLcName();
        Document lcContent = LifecycleUtils.getLifecycleConfiguration(lcName);
        currentLifecycleState.setLcName(lcName);
        currentLifecycleState.setLifecycleId(uuid);
        currentLifecycleState.setState(lcState);
        populateItems(currentLifecycleState, lcContent);
        LifecycleOperationUtil.setCheckListItemData(currentLifecycleState, lifecycleStateBean.getCheckListData());
        return currentLifecycleState;
    }


    /**
     * This method is used to run custom executor codes.
     *
     * @param resource                      The asset to which the lc is attached
     * @return                              success of execution class.
     * @throws LifecycleException  if failed to run custom executors.
     */
    private static boolean runCustomExecutorsCode(Object resource, List<CustomCodeBean> customCodeBeans,
            String currentState, String nextState) throws LifecycleException {
        if (customCodeBeans != null) {
            for (CustomCodeBean customCodeBean : customCodeBeans) {
                if (customCodeBean.getTargetName().equals(nextState)) {
                    Executor customExecutor = (Executor) customCodeBean.getClassObject();
                    customExecutor.execute(resource, currentState, nextState);
                }
            }
        }
        return true;
    }

    private static boolean validateCheckListItemSelected(LifecycleState lifecycleState, String nextState) {
        return !lifecycleState.getCheckItemBeanList().stream()
                .anyMatch(checkItemBean -> checkItemBean.getTargets().contains(nextState) && !checkItemBean.isValue());
    }

    private static boolean validateTargetState (LifecycleState lifecycleState, String nextState) {
        return lifecycleState.getAvailableTransitionBeanList().stream().anyMatch(availableTransitionBean ->
                availableTransitionBean.getTargetState().equals(nextState));
    }
}

