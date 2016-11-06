package com.jupiter.snifferframework.statemachine;

import android.os.Message;
import android.util.Log;

/**
 * Created by wangqiang on 16/8/4.
 */
public class StateMachine {

    public static final String TAG = "StateMachine";

    public StateMachine(String name) {
        mName = name;
        transitionTo(INIT);
    }

    public void transitionTo(IState state) {
        if (state == mCurrentState) return;
        if (mCurrentState != null) {
            mCurrentState.exit();
        }
        mCurrentState = state;
        state.enter();
    }

    public void sendMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = Message.obtain(null, what, arg1, arg2, obj);
        mCurrentState.processMessage(msg);
    }

    public void sendMessage(int what, Object obj) {
        Message msg = Message.obtain(null, what, obj);
        mCurrentState.processMessage(msg);
    }

    public void setInitialState(IState state) {
        transitionTo(state);
    }

    private class InitState extends State {
        @Override
        public void enter() {
            Log.e(TAG, "enter init state");
        }

        @Override
        public boolean processMessage(Message msg) {
            Log.e(TAG, "process message at init state");
            return true;
        }

        @Override
        public void exit() {
            Log.e(TAG, "exit init state");
        }
    }

    private String mName;
    private IState mCurrentState;

    private IState INIT = new InitState();
}
