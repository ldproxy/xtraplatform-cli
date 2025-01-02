export namespace Enums {
  export enum Main {
    Connect = "Connect",
    Info = "Info",
    Check = "Check",
  }
  export enum StoreSubs {
    Cfg = "Cfg",
    Entities = "Entities",
    Layout = "Layout",
  }
  export enum MessageType {
    ERROR = "ERROR",
    WARNING = "WARNING",
    SUCCESS = "SUCCESS",
    INFO = "INFO",
    CONFIRMATION = "CONFIRMATION",
  }
}

export namespace Consts {
  export type Connect = "Connect";
  export type Info = "Info";
  export type Check = "Check";
}

export namespace Options {
  /**
   * @interface true
   */
  export interface Options {
    /**
     * @default ./
     */
    readonly source?: string;
    /**
     * @default false
     */
    readonly verbose?: boolean;
    /**
     * @default false
     */
    readonly debug?: boolean;
  }

  export interface Base extends Options {}

  export interface Store extends Options {
    /**
     * @optional true
     */
    readonly subcommand?: Enums.StoreSubs;
    /**
     * @default false
     */
    readonly ignoreRedundant?: boolean;
    /**
     * @optional true
     */
    readonly path?: string;
  }
}

export namespace Command {
  /**
   * @interface true
   * @discriminator command
   */
  //TODO: should be abstract so that it cannot be instantiated, but that breaks the JSON schema generation
  export class Base {
    readonly command: Enums.Main;
    readonly options: Options.Options;

    constructor(command: Enums.Main, options: Options.Options) {
      this.command = command;
      this.options = options;
    }
  }

  /**
   * @javaContextInit true
   */
  export class Connect extends Base {
    declare readonly command: Enums.Main.Connect;
    declare readonly options: Options.Base;

    constructor(options: Options.Base = {}) {
      super(Enums.Main.Connect, options);
    }
  }

  export class Info extends Base {
    declare readonly command: Enums.Main.Info;
    declare readonly options: Options.Base;

    constructor(options: Options.Base = {}) {
      super(Enums.Main.Info, options);
    }
  }

  export class Check extends Base {
    declare readonly command: Enums.Main.Check;
    declare readonly options: Options.Store;

    constructor(options: Options.Store = {}) {
      super(Enums.Main.Check, options);
    }
  }
}

export namespace Result {
  export interface Message {
    readonly type: Enums.MessageType;
    readonly text: string;
  }

  export interface RegularResult {
    /**
     * @minItems 1
     */
    readonly messages: Message[];
    readonly details?: { [key: string]: any };
  }

  export interface FailureResult {
    readonly error: string;
  }

  /**
   * @interface true
   */
  export type BaseResult = RegularResult | FailureResult;
}
