/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.spring.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ru.org.linux.site.PollNotFoundException;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 23:55:46
 */
public class PollDaoImpl {

  private SimpleJdbcTemplate jdbcTemplate;

  public SimpleJdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PollDTO getCurrentPoll() throws PollNotFoundException {
    String sql = "SELECT votenames.id, votenames.title, votenames.topic FROM votenames" +
      " JOIN topics on votenames.topic = topics.id WHERE" +
      " topics.moderate AND not topics.deleted " +
      " AND topics.commitdate = (select max(commitdate)" +
      " from topics where groupid=19387 AND moderate AND NOT deleted)";
    PollDTO result = jdbcTemplate.queryForObject(sql, new ParameterizedRowMapper<PollDTO>() {
      @Override
      public PollDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        PollDTO result = new PollDTO();
        result.setId(rs.getInt("id"));
        result.setTitle(rs.getString("title"));
        result.setTopic(rs.getInt("topic"));
        return result;
      }
    }, new HashMap());
    if (result == null) {
      throw new PollNotFoundException(-1);
    }
    return result;
  }

  public List<VoteDTO> getVoteDTO(final Integer pollId) {
    String sql = "SELECT id, label FROM votes WHERE vote= ? ORDER BY id";
    return jdbcTemplate.query(sql, new ParameterizedRowMapper<VoteDTO>() {
      @Override
      public VoteDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        VoteDTO dto = new VoteDTO();
        dto.setId(rs.getInt("id"));
        dto.setLabel(rs.getString("label"));
        dto.setPollId(pollId);
        return dto;
      }
    }, pollId);
  }

  public Integer getVotersCount(Integer pollId) {
    String sql = "SELECT sum(votes) as s FROM votes WHERE vote= ?";
    return jdbcTemplate.queryForInt(sql, pollId);
  }

}
