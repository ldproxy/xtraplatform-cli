export namespace Ts2x {
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
    //export type Connect = "Connect";
    //export type Info = "Info";
    //export type Check = "Check";
  }

  export namespace Options {
    /**
     * @interface true
     */
    export class Options {
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

    export class Base extends Options {}

    export class Store extends Options {
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
    export class Command {
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
    export class Connect extends Command {
      declare readonly command: Enums.Main.Connect;
      declare readonly options: Options.Options;

      constructor(options: Options.Options = {}) {
        super(Enums.Main.Connect, options);
      }
    }

    export class Info extends Command {
      declare readonly command: Enums.Main.Info;
      declare readonly options: Options.Base;

      constructor(options: Options.Base = {}) {
        super(Enums.Main.Info, options);
      }
    }

    export class Check extends Command {
      declare readonly command: Enums.Main.Check;
      declare readonly options: Options.Store;

      constructor(options: Options.Store = {}) {
        super(Enums.Main.Check, options);
      }
    }
  }

  export namespace Result {
    export interface Regular {
      /**
       * @minItems 1
       */
      readonly messages: Misc.Message[];
      readonly details?: { [key: string]: any };
    }

    export interface Failure {
      readonly error: string;
    }

    /**
     * @interface true
     */
    export type Result = Regular | Failure;
  }

  export namespace Misc {
    export interface Message {
      readonly status: Enums.MessageType;
      readonly text: string;
    }
  }
}
