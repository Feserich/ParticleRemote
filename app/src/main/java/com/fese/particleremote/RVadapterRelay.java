package com.fese.particleremote;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Fabian on 23.03.2016.
 */

class Relay {
    String relayName;
    String pin;
    boolean isSwitched;

    //TODO: boolean confirmation Popup

    Relay(String relayName, String pin, boolean isSwitched) {
        this.relayName = relayName;
        this.pin = pin;
        this.isSwitched = isSwitched;
    }
}

public class RVadapterRelay extends RecyclerView.Adapter<RVadapterRelay.RelayViewHolder> {

    public interface OnItemClickListener {
        void onItemClicked(String pin);
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

        if (relays.get(position).isSwitched){
            relayViewHolder.relayStatus.setText("Switched");
            //relayViewHolder.relayPhoto.setImageResource(R.drawable.relay_switched);
        }
        else {
            relayViewHolder.relayStatus.setText("Not switched");
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
                        mItemClickListener.onItemClicked(pin);
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
