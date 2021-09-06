import { IonicNativePlugin } from '@ionic-native/core';
/**
 * @name H T Y711
 * @description
 * This plugin does something
 *
 * @usage
 * ```typescript
 * import { HTY711 } from '@ionic-native/hty-711';
 *
 *
 * constructor(private hTY711: HTY711) { }
 *
 * ...
 *
 *
 * this.hTY711.functionName('Hello', 123)
 *   .then((res: any) => console.log(res))
 *   .catch((error: any) => console.error(error));
 *
 * ```
 */
export declare class HTY711 extends IonicNativePlugin {
    /**
     * This function does something
     * @param arg1 {string} Some param to configure something
     * @param arg2 {number} Another param to configure something
     * @return {Promise<any>} Returns a promise that resolves when something happens
     */
    customInput(arg0: any): Promise<any>;
    init(arg0: any): Promise<any>;
    readGiftCard(arg0: any): Promise<any>;
    clearDisplay(arg0: any): Promise<any>;
    displayText(arg0: any): Promise<any>;
    isConnected(arg0: any): Promise<any>;
    startScan(arg0: any): Promise<any>;
    stopScan(arg0: any): Promise<any>;
    confirmTransaction(arg0: any): Promise<any>;
    readCard(arg0: any): Promise<any>;
    getCardInfo(arg0: any): Promise<any>;
    cardReaded(arg0: any): Promise<any>;
    cancelReadCard(arg0: any): Promise<any>;
}
