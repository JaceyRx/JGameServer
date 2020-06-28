package com.jacey.game.battle.repository;

import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.db.entity.BattleRecordEntity;
import com.jacey.game.db.repository.BattleRecordRepository;
import com.jacey.game.db.service.BattleRecordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@RunWith(SpringRunner.class)
@SpringBootTest
public class BattleRecordRepositoryTest {

    @Autowired
    private BattleRecordRepository battleRecordRepository;
    @Autowired
    private BattleRecordService battleRecordService;

    @Test
    public void testSave() {
        BattleRecordEntity battleRecord = new BattleRecordEntity();
        battleRecord.setBattleId("xxxxxxxxxxxxxxx");
        battleRecord.setBattleType(CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer_VALUE);
        battleRecord.setUserIdList("1,2");
        battleRecord.setBattleEndTimestamp(new Date());
        battleRecord.setBattleStartTimestamp(new Date());
        battleRecord.setWinnerUserId(1);
        battleRecordService.saveBattleRecord(battleRecord);
    }

    public static void main(String[] args) {
        List<String> list1 = Arrays.asList("文学","小说","历史","言情","科幻","悬疑");

        List<String> list2 = Arrays.asList("文学","小说","历史","言情","科幻","悬疑");

        List<Integer> list3 = Arrays.asList(1,2,3,4);
        List<String> strUserIds = list3.stream().map(e -> e.toString()).collect(Collectors.toList());

        //方案一：使用String.join()函数，给函数传递一个分隔符合一个迭代器，一个StringJoiner对象会帮助我们完成所有的事情
        String string1 = String.join("-",strUserIds);

        System.out.println(string1);

        //方案二：采用流的方式来写
        String string2 = list2.stream().collect(Collectors.joining("-"));

        System.out.println(string2);

    }

}
