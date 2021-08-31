import { Component, OnDestroy, OnInit } from '@angular/core';
import { HTY711 } from '@ionic-native/hty-711/ngx';
import { Platform } from '@ionic/angular';
import { BehaviorSubject, from, interval, Observable, Subscription } from 'rxjs';
import { filter, map, startWith, switchMap, tap } from 'rxjs/operators';

import { AndroidPermissions } from '@ionic-native/android-permissions/ngx';

declare let window: any;
@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage implements OnInit, OnDestroy {
  loaded$: BehaviorSubject<boolean> = new BehaviorSubject(false);
  isConnected_: boolean = false;
  status: string = "haciendo nada..";
  result: any = {};
  subscriptions: Subscription[] = [];
  bleSubs: Subscription[] = [];
  scanning: boolean = false;

  constructor(
    private hty711: HTY711,
    private platform: Platform,
    private androidPermissions: AndroidPermissions
  ) {}

  ngOnDestroy(){
    this.subscriptions.forEach(sub=>sub.unsubscribe());
    this.bleSubs.forEach(sub=>sub.unsubscribe());
  }

  ngOnInit() {
    this.platform.ready().then((readySource) => {
      console.log('Platform ready from', readySource);
      this.status="plataform listo";
      this.loaded$.next(true);

      this.autoConnectDevice();
      this.checkPermissions();
      
      
      
      // console.log("cordova obj: "); console.dir(JSON.stringify(window.cordova));
      // console.log("cordova plugin: "); console.dir(window.cordova.plugin);
      // console.log("cordova plugins: "); console.dir(window.cordova.plugins);
      // window.plugins.mathcalculator.add({param1: 1, param2: 2}, (result)=>{console.log("result is "+result)}, (error)=>{console.log("error: "+error)});
      // window.plugins.flashlight.available(function (isAvailable) {
      //   if (isAvailable) {

      //     // switch on
      //     window.plugins.flashlight.switchOn(
      //       function () { }, // optional success callback
      //       function () { }, // optional error callback
      //       { intensity: 0.3 } // optional as well
      //     );

      //     // switch off after 3 seconds
      //     setTimeout(function () {
      //       window.plugins.flashlight.switchOff(); // success/error callbacks may be passed
      //     }, 3000);

      //   } else {
      //     alert("Flashlight not available on this device");
      //   }
      // });
    })
  }

  autoConnectDevice(){
    this.subscriptions.push(
      interval(5000).pipe(
        filter(num=>this.scanning == false && this.isConnected_ == false)
      ).subscribe(()=>{
        console.log("autoconnectdevice: ");
        console.log("is scanning: "+this.scanning);
        console.log("is connected: "+this.isConnected_);
        this.connectDevice();
      })
    )
  }

  connectDevice(){
    this.bleSubs.push(
      this.init().pipe(
        switchMap((response)=>{
          console.log(response);
          this.scanning = true;
          return this.startScan();
        }),
        switchMap((response)=>{
          console.log(response);
          return interval(3000).pipe(
            startWith(0),
            switchMap(()=>this.isConnected()),
            tap((isConnected)=>{
              if(isConnected){
                console.log("stoping scan because its already connected")
                this.stopScan().subscribe(()=>{
                  this.scanning = false;
                })
              }
            })
          );
        })
      ).subscribe((isConnected)=>{
        console.log("isconnected: ");console.log(isConnected);
        this.result = isConnected;
        this.isConnected_ = isConnected;
      })
    );
  }

  readGiftCard(){
    if(!this.isConnected_) return;
    console.log("calling readgiftcard");
    this.subscriptions.push(
      from(this.hty711.readGiftCard([500])).subscribe((resp)=>{
        this.result = resp;
      })
    );
  }

  customInput(){
    if(!this.isConnected_) return;
    console.log("calling readgiftcard");
    this.subscriptions.push(
      from(this.hty711.customInput(["Hola", "Que hace? Fedea o que hace?"])).subscribe((resp)=>{
        this.result = resp;
      })
    );
  }

  init(){
    console.log("calling init")
    return from(this.hty711.init([]));
  }

  isConnected():Observable<boolean>{
    console.log("calling isConnected");
    return from(this.hty711.isConnected([])).pipe(
      map(resp=>resp=="true")
    );
  }

  startScan(){
    console.log("calling startScan")
    return from(this.hty711.startScan([]));
  }

  stopScan(){
    console.log("calling stopScan")
    return from(this.hty711.stopScan([]));
  }

  checkPermissions(){

    // <uses-permission android:name="android.permission.INTERNET" />
    // <uses-permission android:name="android.permission.BLUETOOTH" />
    // <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    // <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    // <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    // <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    // <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    // <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.BLUETOTH).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.BLUETOTH)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.INTERNET).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.INTERNET)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.BLUETOOTH_ADMIN).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.BLUETOOTH_ADMIN)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.WRITE_EXTERNAL_STORAGE).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.WRITE_EXTERNAL_STORAGE)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.ACCESS_WIFI_STATE).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.ACCESS_WIFI_STATE)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.ACCESS_NETWORK_STATE).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.ACCESS_NETWORK_STATE)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.ACCESS_COARSE_LOCATION).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.ACCESS_COARSE_LOCATION)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.READ_PHONE_STATE).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.READ_PHONE_STATE)
    );

    this.androidPermissions.checkPermission(this.androidPermissions.PERMISSION.ACCESS_FINE_LOCATION).then(
      result => console.log('Has permission?',result.hasPermission),
      err => this.androidPermissions.requestPermission(this.androidPermissions.PERMISSION.ACCESS_FINE_LOCATION)
    );
    
    this.androidPermissions.requestPermissions([
      this.androidPermissions.PERMISSION.BLUETOTH, 
      this.androidPermissions.PERMISSION.WRITE_EXTERNAL_STORAGE,
      this.androidPermissions.PERMISSION.ACCESS_WIFI_STATE,
      this.androidPermissions.PERMISSION.ACCESS_NETWORK_STATE,
      this.androidPermissions.PERMISSION.ACCESS_COARSE_LOCATION,
      this.androidPermissions.PERMISSION.READ_PHONE_STATE,
      this.androidPermissions.PERMISSION.ACCESS_FINE_LOCATION,
      this.androidPermissions.PERMISSION.INTERNET,
    ]);
  }
}
