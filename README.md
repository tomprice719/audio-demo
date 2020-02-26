A simple demo of the code I’ve been using to write music.

To run:

lein repl

This will cause a small blank window to pop up. You can play notes by typing keys on your keyboard while this window is in focus. The keys used for this, in order of ascending pitch, are “q” to “?” on the first row, “a” to “;” on the second row, and  “q” to “p” on the third row.

There are also other keyboard commands available when this window is in focus. They are:

Space bar - switch between scales

“1” - play the current recording.

“2” - play the current recording while recording anything that you are currently playing and layering it on top.

“3” - stop playing and recording.

“4” - print the time since the beginning of the recording.

“5” to “8” - these keys are used to switch instruments.

There are some functions you can call at the repl:

(play), (rec), and (stop) - these are the same as the keyboard commands “1” to “3” respectively.

(get-time) - same as the “4” command.

(set-time t) - changes the time that the recording starts to play from to t, where t is time in seconds.

(save) - saves the current recording to the “saved” folder, with a name automatically generated by a counter. The most recent saved recording is automatically loaded next time you start, and you can also load a saved recording using (load).

(load file-num) - Loads the recording with the specified number. If no file-num argument is provided, loads the most recent recording.

(clear) - clears all notes from the current recording.

(rec-wav) - starts playing the recording, and recording all generated audio to a wav file that is saved when you stop. The file is given an automatically generated name based on the timestamp of when you started recording, and is saved in the “saved” folder.

(expand start-time interval) - expands the duration of the recording by inserting an empty gap at some point. Here interval is the duration of the gap in seconds and start-time is where it begins in seconds from the start of the recording.

(remove-instrument min-time max-time removed-key) - removes from the recording the instrument with key instrument-key, within a time window bounded by min-time and max-time, which are given in seconds.

(volume vol) - set the master volume.

Potential problem

If you get any popping or clicking, try increasing the fft-size var in overtone-utils to a larger power of 2. This makes things easier on the CPU at the expense of greater latency.
