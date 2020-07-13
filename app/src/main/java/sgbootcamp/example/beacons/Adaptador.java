package sgbootcamp.example.beacons;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class Adaptador extends BaseAdapter {

    private static LayoutInflater inflater = null;
    ArrayList <BeaconModel> beaconArrayList;
    Context context;

    public Adaptador(Context context, ArrayList <BeaconModel> beaconArrayList ) {
        this.context = context;
        this.beaconArrayList = beaconArrayList ;

        inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
    }

    public ArrayList<BeaconModel> getBeaconArrayList() {
        return beaconArrayList;
    }

    @Override
    public int getCount() {
        return beaconArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final View view = inflater.inflate(R.layout.beacon_card,null);
        TextView namespaceID = (TextView) view.findViewById(R.id.namespaceID);
        TextView instanceID = (TextView) view.findViewById(R.id.instanceID);
        TextView bateryId = (TextView) view.findViewById(R.id.bateryId);
        TextView distanceID = (TextView) view.findViewById(R.id.distanceID);
        TextView rssiId = (TextView) view.findViewById(R.id.rssiId);
        TextView txId = (TextView) view.findViewById(R.id.txId);

        namespaceID.setText(beaconArrayList.get(i).getNamespaceID());
        instanceID.setText(beaconArrayList.get(i).getInstanceID());
        bateryId.setText(beaconArrayList.get(i).getBateryId());
        distanceID.setText(beaconArrayList.get(i).getDistanceID());
        rssiId.setText(beaconArrayList.get(i).getRssiId());
        txId.setText(beaconArrayList.get(i).getTxId());

        return view;
    }
}
