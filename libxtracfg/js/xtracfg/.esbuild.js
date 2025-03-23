import { build } from "esbuild";
import { clean } from "esbuild-plugin-clean";

await build({
  entryPoints: ["src/index.ts"],
  bundle: false,
  platform: "node",
  target: "node20",
  format: "esm",
  minify: true,
  //logLevel: "debug",
  /*loader: {
    ".node": "copy",
  },*/
  outdir: "build",
  outbase: "node_modules",
  entryNames: "[name]",
  define: {
    "process.env.NODE_ENV": '"production"',
  },
  plugins: [
    clean({
      patterns: ["./build/*"],
    }),
  ],
});
