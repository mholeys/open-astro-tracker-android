package uk.co.mholeys.android.openastrotracker_control.ui.control;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.mholeys.android.openastrotracker_control.ISearcherControl;
import uk.co.mholeys.android.openastrotracker_control.MountViewModel;
import uk.co.mholeys.android.openastrotracker_control.PolarAlignmentDialogFragment;
import uk.co.mholeys.android.openastrotracker_control.PolarAlignmentDialogFragment2;
import uk.co.mholeys.android.openastrotracker_control.R;
import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.MountMessages;

public class ControlFragment extends Fragment {

    private static final String TAG = "CtrlFrag";
    Button mStopTrackingButton;
    Button mStartTrackingButton;
    TextView mTrackingStateView;

    Button mMoveUpButton;
    Button mMoveLeftButton;
    Button mMoveDownButton;
    Button mMoveRightButton;

    Button mHomeButton;
    Button mSetHomeButton;
    Button mSlewToButton;
    Button mPolarAlignButton;
    Button mDisconnectButton;
    Button mGetPositionButton;

    TextView mCurrentRAText;
    TextView mCurrentDECText;

    private MountViewModel mountViewModel;
    private ISearcherControl control;
    private Messenger serviceMessenger;

    private PolarAlignmentDialogFragment.PolarAlignListener polarAlignmentListener = new PolarAlignmentDialogFragment.PolarAlignListener() {

        boolean movedHome = false;

        @Override
        public void onHome(Dialog dialog) {
            // Move to home position
            if (isBound()) {
                MountMessages.goHome(serviceMessenger);
            }
            Toast.makeText(getContext(), R.string.moving_to_home_wait, Toast.LENGTH_LONG).show();
            movedHome = true;
            // Tell user to wait for homing
            AlertDialog alertDialog = (AlertDialog) dialog;
            Button homeButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            homeButton.setEnabled(false);
            homeButton.setText(R.string.homing);
            alertDialog.setTitle(R.string.wait_for_home_then_align);
        }

        @Override
        public void onAlign(Dialog dialog) {
            // Move to polar position
            // TODO:
            if (isBound()) {
                // TODO: north/south hemi
                Mount.HMS polarisRa = new Mount.HMS(2, 58, 51);
                Mount.HMS polarisDecNorth = new Mount.HMS(88, 42, 12);
                Mount.HMS polarisDecSouth = new Mount.HMS(-88, 41, 12);
                TelescopePosition polarisPos = new TelescopePosition(Mount.HMSToFloat(polarisRa), Mount.HMSToFloat(polarisDecNorth), OTAEpoch.JNOW);
                MountMessages.slew(serviceMessenger, polarisPos);
                // GoTo Polaris
                //":Sr02:58:51#,n";
                // Northern
                //":Sd+88*42:12#,n";
                // Southern
                //":Sd-88*42:12#,n";
                //":MS#,n";
            }
            // Show next dialog
            dialog.dismiss();
            PolarAlignmentDialogFragment2 secondDialog = new PolarAlignmentDialogFragment2();
            secondDialog.show(getParentFragmentManager(), "PolarAlignmentDialogFragment2");
            secondDialog.setPolarAlignmentListener(polarAlignmentListener2);
            reset();
        }

        @Override
        public void onCancel(Dialog dialog) {
            // User has cancelled the alignment, probably due to home not being set
            // If they have clicked move home followed by cancel the home position may be unset
            if (movedHome) {
                Toast.makeText(getContext(), R.string.home_may_be_unset, Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
            reset();
        }

        public void reset() {
            movedHome = false;
        }

    };

    private PolarAlignmentDialogFragment2.PolarAlignListener polarAlignmentListener2 = new PolarAlignmentDialogFragment2.PolarAlignListener() {

        @Override
        public void onDone(Dialog dialog) {
            // User has stated that polaris is now in the center of the screen
            if (isBound()) {
                Mount.HMS polarisRa = new Mount.HMS(2, 58, 51);
                Mount.HMS polarisDecNorth = new Mount.HMS(89, 21, 06);
                Mount.HMS polarisDecSouth = new Mount.HMS(-89, 21, 06);
                TelescopePosition polarisPos = new TelescopePosition(Mount.HMSToFloat(polarisRa), Mount.HMSToFloat(polarisDecNorth), OTAEpoch.JNOW);
                MountMessages.syncPolaris(serviceMessenger, polarisPos);
                MountMessages.setTracking(serviceMessenger, true);
                // Northern
                //":SY+89*21:06.02:58:51#,n";
                // Southern
                //":SY-89*21:06.02:58:51#,n";
            }
        }

        @Override
        public void onCancel(Dialog dialog) {
            Toast.makeText(getContext(), R.string.home_may_be_unset, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    };

    ScheduledExecutorService movementExecutor;
    private ScheduledFuture<?> movementRunnableFuture;
    private MovementRunnable movementRunnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_control, container, false);

        mStopTrackingButton = root.findViewById(R.id.tracking_off_btn);
        mStopTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopTrackingClick(v);
            }
        });
        mStartTrackingButton = root.findViewById(R.id.tracking_on_btn);
        mStartTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartTrackingClick(v);
            }
        });
        mTrackingStateView = root.findViewById(R.id.tracking_text);
        movementExecutor = Executors.newSingleThreadScheduledExecutor();

        mMoveUpButton = root.findViewById(R.id.move_up_btn);
        mMoveUpButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    move('N');
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopMovement();
                    return true;
                }
                return false;
            }
        });
//        mMoveUpButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onUpClick(v);
//            }
//        });
        mMoveLeftButton = root.findViewById(R.id.move_left_btn);
        mMoveLeftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    move('W');
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopMovement();
                    return true;
                }
                return false;
            }
        });
//        mMoveLeftButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onLeftClick(v);
//            }
//        });
        mMoveDownButton = root.findViewById(R.id.move_down_btn);
        mMoveDownButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    move('S');
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopMovement();
                    return true;
                }
                return false;
            }
        });
//        mMoveDownButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onDownClick(v);
//            }
//        });
        mMoveRightButton = root.findViewById(R.id.move_right_btn);
        mMoveRightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    move('E');
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopMovement();
                    return true;
                }
                return false;
            }
        });
//        mMoveRightButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onRightClick(v);
//            }
//        });

        mHomeButton = root.findViewById(R.id.home_btn);
        mHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHomeClick(v);
            }
        });
        mSetHomeButton = root.findViewById(R.id.set_home_btn);
        mSetHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetHomeClick(v);
            }
        });
        mSlewToButton = root.findViewById(R.id.slew_to_btn);
        mSlewToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSlewToClick(v);
            }
        });

        mDisconnectButton = root.findViewById(R.id.disconnect_btn);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisconnectClick(v);
            }
        });

        mGetPositionButton = root.findViewById(R.id.get_pos_btn);
        mGetPositionButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  if (!isBound()) return;
                  MountMessages.getPosition(serviceMessenger);
              }
          });

        mCurrentRAText = root.findViewById(R.id.current_ra_text);
        mCurrentDECText = root.findViewById(R.id.current_dec_text);
        mPolarAlignButton = root.findViewById(R.id.polar_align_btn);
        mPolarAlignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PolarAlignmentDialogFragment dialog = new PolarAlignmentDialogFragment();
                dialog.show(getParentFragmentManager(), "PolarAlignmentDialogFragment");
                dialog.setPolarAlignmentListener(polarAlignmentListener);
            }
        });
                
        return root;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof ISearcherControl) {
            control = (ISearcherControl) getActivity();
            this.serviceMessenger = control.getMessenger();
        } else {
            Log.e(TAG, "onActivityCreated: Activity doesn't implement ISearcherControl!");
        }

        mountViewModel = new ViewModelProvider(this).get(MountViewModel.class);
    }

    private boolean isBound() {
        return control.isBound() && serviceMessenger != null;
    }

    //Buttons
    public void onUpClick(View view) {
        if (!isBound()) return;
        MountMessages.moveSlightly(serviceMessenger, 'N', 50);
    }

    public void onDownClick(View view) {
        if (!isBound()) return;
        MountMessages.moveSlightly(serviceMessenger, 'S', 50);
    }

    public void onLeftClick(View view) {
        if (!isBound()) return;
        MountMessages.moveSlightly(serviceMessenger, 'W', 50);
    }

    public void onRightClick(View view) {
        if (!isBound()) return;
        MountMessages.moveSlightly(serviceMessenger, 'E', 50);
    }

    public void onStopTrackingClick(View view) {
        if (!isBound()) return;
        MountMessages.setTracking(serviceMessenger, false);
    }

    public void onStartTrackingClick(View view) {
        if (!isBound()) return;
        MountMessages.setTracking(serviceMessenger, true);
    }

    public void onHomeClick(View view) {
        if (!isBound()) return;
        MountMessages.goHome(serviceMessenger);
    }

    public void onSetHomeClick(View view) {
        if (!isBound()) return;
        MountMessages.setHome(serviceMessenger);
    }

    public void onSlewToClick(View view) {
        if (!isBound()) return;
    }

    public void onDisconnectClick(View view) {
        if (!isBound()) return;
        control.disconnect();
    }


    private void stopMovement() {
        if (movementRunnableFuture != null && !movementRunnableFuture.isCancelled()) {
            movementRunnableFuture.cancel(true);
        }
    }

    private void move(char direction) {
        stopMovement();
        if (movementRunnable == null) {
            movementRunnable = new MovementRunnable(direction);
        } else {
            movementRunnable.direction = direction;
        }
        movementRunnable = new MovementRunnable(direction);
        movementRunnableFuture = movementExecutor.scheduleAtFixedRate(movementRunnable, 0, MovementRunnable.DEFAULT_DURATION, TimeUnit.MILLISECONDS);
    }


    class MovementRunnable implements Runnable {

        public static final int DEFAULT_DURATION = 200;

        char direction;
        int duration = DEFAULT_DURATION;

        public MovementRunnable(char direction) {
            this.direction = direction;
        }

        public void run() {
            /* TODO switch to calculating offset from current target location, and move to that instead
             * TODO That way it will move faster, this is way too slow of movement
             */
            MountMessages.moveSlightly(serviceMessenger, direction, duration);
        }

    }

}