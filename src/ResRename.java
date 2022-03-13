import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import bin.arsc.ArscFile;
import bin.zip.ZipEntry;
import bin.zip.ZipFile;
import bin.zip.ZipOutputStream;

public class ResRename {

    public static ArrayList<String> nxhdpiList = new ArrayList<String>();//是否存在-nxhdpi
    public static ArrayList<String> hugeList = new ArrayList<String>();//是否存在huge
    public static ArrayList<String> godList = new ArrayList<String>();//是否存在god
    //public static ArrayList<String> resOriginalDirNameList = new ArrayList<String>();//原版资源文件夹列表

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: java -jar arr.jar original.apk build.apk out.apk");
        } else {
            String original = new File(args[0]).getAbsolutePath();
            resrename(new File(original), new File(args[1]), new File(args[2]));
        }
    }

    public static HashMap<String, String> getOriginalResNameFromZip(File file) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<ZipEntry> entryEnumeration = zipFile.getEntries();
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement();
                if (z.isDirectory())
                    continue;
                if(z.getName().startsWith("res/")) {
                    String dirName = new File(z.getName()).getParent();
                    //String baseName = new File(z.getName()).getName();
                    map.put(z.getName(), z.getName());
                    /*if (!resOriginalDirNameList.contains(dirName))
                        resOriginalDirNameList.add(dirName);*/
                    if (dirName.contains("-nxhdpi"))
                        nxhdpiList.add(z.getName());
                    if (dirName.contains("-hugeui"))
                        hugeList.add(z.getName());
                    if (dirName.contains("-godzillaui"))
                        godList.add(z.getName());
                }
            }
            zipFile.close();
        } catch (Throwable e) {
            throw new Exception("E: " + file + " is a bad apk file!");
        }
        return(map);
    }

    private static void resrename(File originalApk, File buildApk, File outApk) throws Exception {
        //获取原版资源名称
        HashMap<String, String> originalResMap = getOriginalResNameFromZip(originalApk);
        //开始
        ZipFile zipFile = null;
        ZipOutputStream zos = null;
        File mapping = null;
        FileOutputStream fos = null;
        try {
            //获取apktool编译后apk的资源路径
            zipFile = new ZipFile(buildApk);
            HashMap<String, String> map = new HashMap<>();
            Enumeration<ZipEntry> entryEnumeration = zipFile.getEntries();
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement();
                if (z.isDirectory())
                    continue;
                if(z.getName().startsWith("res/"))
                    map.put(z.getName(), z.getName());
            }
            //读取resources.arsc数据
            ZipEntry arsc = zipFile.getEntry("resources.arsc");
            if (arsc == null || arsc.isDirectory())
                throw new IOException("resources.arsc not found");
            byte[] data = new byte[(int) arsc.getSize()];
            InputStream is = zipFile.getInputStream(arsc);
            int start = 0;
            int len;
            while (start < data.length &&(len = is.read(data, start, data.length - start)) > 0)
                start += len;
            if (start != data.length)
                throw new IOException("E: Read resources.arsc error");
            ArscFile arscFile = ArscFile.decodeArsc(new ByteArrayInputStream(data));
            //重命名res内文件
            System.out.println("I: Starting resources rename...");
            for (int i = 0; i < arscFile.getStringSize(); i++) {
                String s = arscFile.getString(i);//获取编译后app的资源路径
                if (s.startsWith("res/") && map.containsKey(s)) {//检查是否为有效路径
                    String newName = s;
                    for (String resOriginalPath : originalResMap.keySet()) {//循环提取原包资源
                        //System.out.println(resOriginalPath);
                        String resOriginalDir = new File(resOriginalPath).getParent();//res/drawable-hdpi-v4
                        String resOriginalFile = new File(resOriginalPath).getName();
                        String version = "";
                        String resNewName = new File(s).getParent();
                        String resNewName2 = new File(s).getParent();
                        if (resOriginalDir.contains("-")) {
                            //System.out.println(resOriginalDir.split("-")[1]);
                            String[] splitStrings = resOriginalDir.split("-");
                            version = splitStrings[splitStrings.length - 1];//v4
                            resNewName = resOriginalDir.replace("-" + version, "") + "/" + resOriginalFile;//res/drawable-hdpi/xxx.png
                            resNewName2 = resOriginalDir.split("-")[0] + "/" + resOriginalFile;//res/drawable/xxx.png
                        }
                        if (!resNewName.isEmpty()) {
                            String resNewDirName = new File(resNewName).getParent();
                            String resNewFileName = new File(resNewName).getName();
                            //String resNewDirName2 = new File(resNewName2).getParent();//res/drawable不需要替换-nxhdpi为-440dpi
                            //String resNewFileName2 = new File(resNewName2).getName();
                            String sdir = new File(s).getParent();
                            //String sfile = new File(s).getName();
                            //替换-nxhdpi为-440dpi
                            if (nxhdpiList.size() != 0 && sdir.contains("-440dpi")) {
                                resNewName = resNewDirName.replace("-nxhdpi", "-440dpi") + "/" + resNewFileName;
                                resNewDirName = new File(resNewName).getParent();
                                resNewFileName = new File(resNewName).getName();
                            }
                            //替换-hugeui和"-godzillaui"为-uiModeType=0
                            if (hugeList.size() != 0 && sdir.contains("-uiModeType=0")) {
                                resNewName = resNewDirName.replace("-hugeui", "-uiModeType=0") + "/" + resNewFileName;
                                //替换-hugeui和"-godzillaui"为-uiModeType=0
                            } else if (godList.size() != 0 && sdir.contains("-uiModeType=0")) {
                                resNewName = resNewDirName.replace("-godzillaui", "-uiModeType=0") + "/" + resNewFileName;
                            }
                        }
                        //是否匹配
                        boolean isEquals = false;
                        if (resNewName.equals(s) || resNewName2.equals(s))
                            isEquals = true;
                        if (version.startsWith("v") && isEquals)
                            newName = resOriginalPath;
                        //var
                        String p;
                        String d;
                        //替换-440dpi为-nxhdpi
                        if (nxhdpiList.size() != 0) {
                            p = new File(newName).getParent();
                            d = new File(newName).getName();
                            newName = p.replace("-440dpi", "-nxhdpi") + "/" + d;//还原-440dpi为原包的-nxhdpi
                        }
                        //替换-uiModeType=0为-hugeui
                        if (hugeList.size() != 0) {
                            p = new File(newName).getParent();
                            d = new File(newName).getName();
                            newName = p.replace("-uiModeType=0", "-hugeui") + "/" + d;//还原-uiModeType=0为原包的-hugeui
                        } else if (godList.size() != 0) {//替换-uiModeType=0为-godzillaui
                            p = new File(newName).getParent();
                            d = new File(newName).getName();
                            newName = p.replace("-uiModeType=0", "-godzillaui") + "/" + d;//还原-uiModeType=0为原包的-godzillaui
                        }
                        //退出循环
                        if (isEquals) {
                            break;
                        }
                    }
                    arscFile.setString(i, newName);
                    map.put(s, newName);
                }
            }

            //写出压缩包
            String name = outApk.getName();
            int i = name.lastIndexOf('.');
            if (i != -1) {
                mapping = new File(outApk.getParentFile(), name.substring(0, i) + "_mapping.txt");
                //name = name.substring(0, i) + "_" + name.substring(i);
            } else {
                mapping = new File(outApk.getParentFile(), name + "_mapping.txt");
                //name += "_";
            }

            zos = new ZipOutputStream(outApk);
            //resources.arsc最好不要进行压缩
            zos.setMethod(ZipOutputStream.STORED);
            zos.putNextEntry("resources.arsc");
            zos.write(ArscFile.encodeArsc(arscFile));

            entryEnumeration = zipFile.getEntries();
            fos = new FileOutputStream(mapping);
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement();
                //文件夹不需要写出
                if (z.isDirectory() || z.getName().equals("resources.arsc") || z.getName().startsWith("META-INF/"))
                    continue;
                if (map.containsKey(z.getName())) {
                	String na = map.get(z.getName());
                	fos.write((z.getName() + " -> " + na + "\n").getBytes());
                    z.setName(na);
                }

                //复制原始压缩数据，无需解压再压缩
                zos.copyZipEntry(z, zipFile);
            }
            fos.close();
            zos.close();
            zipFile.close();
            System.out.println("I: Resources rename succeeded!");
        } catch (Throwable e) {
            e.printStackTrace();
            try {
            	if (zipFile != null)
                    zipFile.close();
            } catch (IOException ioe){}
            try {
                if (zos != null)
                    zos.close();
            } catch (IOException ioe){}
            try {
                if (fos != null)
                	fos.close();
            } catch (IOException ioe){}
            if (outApk != null && outApk.exists())
                outApk.delete();
            if (mapping != null && mapping.exists())
            	mapping.delete();
        }
    }

}
