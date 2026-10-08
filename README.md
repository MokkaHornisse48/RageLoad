# RageLoad
RageLoad is a Minecraft mod that I have been working on for quite some time now.
It's currently not meant for production use because I still need to finish the config.
Also, there are still a lot of bugs.

## History
This mod started as a thought experiment. Imagine trying to fit 1000 players on a VPS with 8 GB of RAM and with 100 mods.
What optimizations would be required to achieve this?
Then my friends made a Minecraft server with way too many mods.
Chunks took seconds to generate.
Turns out Tectonic and Oh The Biomes We've Gone completely crush world generation speeds.
Also, for some reason, C2ME just crashed the server.
Then, because of boredom, I looked through the Minecraft Bedrock Edition protocol documentation and found a feature that really interested me.
Client-side world estimation.
So I tried to implement this myself on java.
This is not my first attempt.
First, I tried to completely recreate Minecraft world generation because the worldgen code was too reliant on the server.
But doing it this way would be incredibly hard and break compatibility with a lot of mods.
So I tried something new.
This time I created a fake MinecraftServer instance on the client.
After two weeks of fighting, I finally managed to initialize a Minecraft server on the client that also generates chunks.
But it had terrible performance and caused a lot of stutter.
So I learned how to use JDK Mission Control to find the cause.
I also had a lot of trouble with Architectury. So I moved to MultiLoader.
This project also suffered a lot of feature creep.
For example, inspired by the OpenCL module of C2ME, I tried to do the same.
I thought using OpenCL on the client instead of the server would make a lot more sense.
I actually managed to reimplement all density functions in OpenCL.
I wrote a test and all density functions worked exactly like vanilla.
But the compile times were terrible. So I started again.
This time I tried to do it without code compilation.
Turning the density functions into bytecode that is then interpreted on the GPU.
At the end, I never finished it, and this project completely burned me out.
So I took a break.
After the break I completely removed the OpenCL code.
Instead I had a new idea.
The worldgen doesn't have to be perfect on the client.
For example, we do not need to generate caves, and biomes only need to be 2D and surface-bound.
This worked great but made the mod even more inaccurate.
But it works and it makes the worldgen multiple times faster.
It also introduced a lot of visual inaccuracies.
Most of them are fixed now.

## Now
I currently lack some motivation to just finish this.
I still find bugs regularly.
Maybe I will finish it someday.
For my sanity's sake, I currently want to focus on other modding projects.

## What still needs to be done
Add config and config UI.
Fix bugs where trees generate differently on the client than on the server.
Fix overhangs not working with optimizations.
Fix structure generation.
Rework server chunk to client syncing.
