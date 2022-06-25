package com.englishcenter.member.async;

import com.englishcenter.core.firebase.IFirebaseFileService;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.member.command.CommandUpdateScoreByExcel;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class UpdateScoreAsync {
    @Autowired
    private IFirebaseFileService firebaseFileService;
    @Autowired
    private MemberApplication memberApplication;

    @Async
    public void consumerUpdateScore(CommandUpdateScoreByExcel command) {
        try {
            URL website = new URL(firebaseFileService.getDownloadUrl(command.getPath(), "imports"));
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(command.getPath());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();

            File _file = new File(command.getPath());
            FileInputStream fis = null;
            Workbook workbook = null;
            try {
                fis = new FileInputStream(_file);
                workbook = WorkbookFactory.create(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    Cell cellEmail = row.getCell(0);
                    Cell cellRead = row.getCell(1);
                    Cell cellListen = row.getCell(2);
                    Cell cellType = row.getCell(3);
                    try {
                        String email = cellEmail.getStringCellValue();
                        float read = (float) cellRead.getNumericCellValue();
                        float listen = (float) cellListen.getNumericCellValue();
                        String type = cellType.getStringCellValue();
                        Optional<Member> member = memberApplication.getByCode(email);
                        if (!member.isPresent() || !Member.MemberType.STUDENT.equals(member.get().getType())) {
                            throw new Exception();
                        }
                        Member student = member.get();
                        Member.Score score = Member.Score.builder()
                                .listen(listen)
                                .read(read)
                                .total(listen + read)
                                .build();
                        if ("in".equals(type)) {
                            student.setInput_score(score);
                        }

                        student.setCurrent_score(score);

                        if (CollectionUtils.isEmpty(student.getLog_score())) {
                            student.setLog_score(new ArrayList<>());
                        }
                        student.getLog_score().add(Member.LogScore.builder()
                                .date(System.currentTimeMillis())
                                .score(score)
                                .build());

                        memberApplication.mongoDBConnection.update(student.get_id().toHexString(), student);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            }
            workbook.close();
            fis.close();
            _file.delete();
            firebaseFileService.delete("imports/" + command.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
