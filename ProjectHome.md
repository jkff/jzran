### What is it? ###
It's a Java library based on the zran.c sample from zlib.

You can preprocess a large gzip archive, producing an "index" that can be used for random read access.

You can balance between index size and access speed.

### What can I use it for? ###
You've got a file that is very large, compressible and needs random access: some kind of database, DNA, image, video, XML document etc.

You go through it and remember offsets of what is important to you.

You compress the file.

Now you can use these offsets to access the compressed file.

### How to use it? ###
You give it a `SeekableInputStream` over the compressed data.

It gives you a `SeekableInputStream` over the decompressed data.
```
SeekableInputStream sis = new ByteArraySeekableInputStream(buf);
SeekableInputStream index = RandomAccessGZip.index(sis, 1048576);

...

index.open(sis);

...

index.seek(offset);

byte[] dest = new byte[100];
int n = index.read(dest, 0, dest.length);
```

### What else is there? ###
You can monitor indexing progress and cancel indexing.

The index is serializable.

You can provide as input (gzip source) a `byte[]`, a `ByteBuffer` or a `RandomAccessFile`.

### How does it work? ###
zran just snapshots the decoder's internal state periodically.

### How fast is it? ###
I haven't yet done measurements, but essentially the `seek` method is O(span) (the sparser your index, the smaller it is and the slower seeks work) and after a seek, you read with the speed of zlib (modulo a couple of memory copies maybe).