import { build } from "esbuild";
import { clean } from "esbuild-plugin-clean";

const common = {
  entryPoints: ["src/index.ts"],
  bundle: true,
  format: "esm",
  minify: true,
  outdir: "build",
  outbase: "node_modules",
  define: {
    "process.env.NODE_ENV": '"production"',
  },
};

await build({
  ...common,
  platform: "node",
  target: "node20",
  entryNames: "[name]",
  plugins: [
    clean({
      patterns: ["./build/*"],
    }),
  ],
});

await build({
  ...common,
  platform: "browser",
  target: "esnext",
  entryNames: "[name].browser",
});
