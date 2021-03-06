package com.fese.particleremote;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;

/**
 * Created by Fabian on 23.03.2016.
 */

class Relay {
    String relayName;
    String pin;
    boolean isSwitched;
    boolean tryToSwitch;
    boolean switchConfirmation;
    Integer toggleTime;


    Relay(String relayName, String pin, boolean isSwitched, boolean switchConfirmation, boolean tryToSwitch, Integer toggleTime) {
        this.relayName = relayName;
        this.pin = pin;
        this.tryToSwitch = tryToSwitch;
        this.isSwitched = isSwitched;
        this.switchConfirmation = switchConfirmation;
        this.toggleTime = toggleTime;
    }
}

public class RVadapterRelay extends RecyclerView.Adapter<RVadapterRelay.RelayViewHolder> {

    public interface OnItemClickListener {
        void onItemClicked(String pin) throws ParticleCloudException;
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClicked(String pin);
    }




    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


    @Override
    public RelayViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_relay, viewGroup, false);
        RelayViewHolder rvh = new RelayViewHolder(v);
        return rvh;
    }

    @Override
    public void onBindViewHolder(RelayViewHolder relayViewHolder, int position) {
        relayViewHolder.relayName.setText(relays.get(position).relayName);
        relayViewHolder.relayPin.setText(relays.get(position).pin);


        if(relays.get(position).tryToSwitch){
            relayViewHolder.relayStatus.setText("switching...");
            relayViewHolder.relayPhoto.setImageResource(R.drawable.ic_led_off_grey600_48dp);
        }
        else if (relays.get(position).isSwitched){
            relayViewHolder.relayStatus.setText("energized");
            relayViewHolder.relayPhoto.setImageResource(R.drawable.ic_led_on_grey600_48dp);
        }
        else {
            relayViewHolder.relayStatus.setText("de-energized");
            relayViewHolder.relayPhoto.setImageResource(R.drawable.ic_led_variant_off_grey600_48dp);
        }

    }

    @Override
    public int getItemCount() {
        return relays.size();
    }

    public static class RelayViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView relayName;
        TextView relayPin;
        TextView relayStatus;
        ImageView relayPhoto;
        View view;


        private static OnItemClickListener mItemClickListener;
        public static void setOnItemClickListener(OnItemClickListener l) {
            mItemClickListener = l;
        }

        private static OnItemLongClickListener mItemLongClickListener;
        public static void setOnItemLongClickListener(OnItemLongClickListener l) {
            mItemLongClickListener = l;
        }


        public RelayViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.cv_relay);
            relayName = (TextView) itemView.findViewById(R.id.relayName);
            relayPin = (TextView) itemView.findViewById(R.id.relayPin);
            relayStatus = (TextView) itemView.findViewById(R.id.relayStatus);
            relayPhoto = (ImageView) itemView.findViewById(R.id.relayPhoto);
            view = itemView;

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View itemView) {
                    if (mItemClickListener != null) {
                        final String pin = relayPin.getText().toString();
                        try {
                            mItemClickListener.onItemClicked(pin);
                        } catch (ParticleCloudException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if (mItemLongClickListener != null){
                        final String pin = relayPin.getText().toString();
                        mItemLongClickListener.onItemLongClicked(pin);
                    }

                    return true;
                }
            });

        }
    }

    List<Relay> relays;
    RVadapterRelay(List<Relay> relays){
        this.relays = relays;
    }


}
