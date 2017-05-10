package com.example.martin.ptbt;

import android.os.Binder;

/**
 * Created by Martin on 10.05.2017.
 */

public class GattClientService {



    public class LocalBinder extends Binder {
        public GattClientService getService() {
            return GattClientService.this;
        }
    }
}
